package com.omnidapt.pd

import com.omnidapt.pd.data.MockRepository
import com.omnidapt.pd.data.DeviceConnectionState
import com.omnidapt.pd.data.DoctorPatientRecord
import com.omnidapt.pd.data.BaselineSamplingState
import com.omnidapt.pd.data.ElectrodeSelection
import com.omnidapt.pd.data.ExportFormat
import com.omnidapt.pd.data.ExportSettings
import com.omnidapt.pd.data.FrequencyBands
import com.omnidapt.pd.data.ImpedanceMode
import com.omnidapt.pd.data.ImpedanceSide
import com.omnidapt.pd.data.InitializationStep
import com.omnidapt.pd.data.ParameterOptimizationSettings
import com.omnidapt.pd.data.PatientListGroup
import com.omnidapt.pd.data.PatientSortField
import com.omnidapt.pd.data.SymptomFeedback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockRepositoryTest {
    @Test
    fun submitSymptomFeedbackUpdatesLatestReport() {
        val repository = MockRepository()
        val patient = repository.getCurrentPatient()

        repository.submitSymptomFeedback(SymptomFeedback(tremor = 2, rigidity = 1, speech = 0))

        assertEquals(2, repository.getPatientReport(patient.id).latestFeedback.tremor)
        assertEquals(1, repository.getPatientReport(patient.id).latestFeedback.rigidity)
    }

    @Test
    fun markMedicationTakenUpdatesEveningRecord() {
        val repository = MockRepository()
        val patient = repository.getCurrentPatient()

        repository.markMedicationTaken(patient.id, timestamp = 0L)

        assertTrue(repository.getPatientReport(patient.id).medications.last().eveningTaken)
    }

    @Test
    fun confirmParameterDownloadAddsNewestHistoryRecord() {
        val repository = MockRepository()
        val patient = repository.getCurrentPatient()
        val suggestion = repository.getOptimizationSuggestion(patient.id)

        repository.confirmParameterDownload(patient.id, suggestion.suggestedParameters)

        val newest = repository.getPatientReport(patient.id).parameterHistory.first()
        assertEquals(suggestion.suggestedParameters.currentMa, newest.currentMa)
        assertEquals(suggestion.suggestedParameters.frequencyHz, newest.frequencyHz)
    }

    @Test
    fun parameterHistoryIsIsolatedPerDoctorPatient() {
        val repository = MockRepository()
        val firstPatient = "2403021728"
        val secondPatient = "2306151123"
        val secondBefore = repository.getPatientReport(secondPatient).parameterHistory.size
        val suggestion = repository.getOptimizationSuggestion(firstPatient)

        repository.confirmParameterDownload(firstPatient, suggestion.suggestedParameters)

        assertEquals(
            suggestion.suggestedParameters,
            repository.getPatientReport(firstPatient).parameterHistory.first()
        )
        assertEquals(secondBefore, repository.getPatientReport(secondPatient).parameterHistory.size)
    }

    @Test
    fun addDoctorPatientAddsRecordAndRejectsDuplicateNumber() {
        val repository = MockRepository()
        val before = repository.getDoctorPatients().size
        val record = DoctorPatientRecord(
            id = "2607250001",
            name = "测试患者",
            gender = "男",
            age = 60,
            number = "2607250001",
            implantDate = "2026/07/25",
            summary = "新增患者用于列表测试",
            group = PatientListGroup.PendingInitialization
        )

        assertTrue(repository.addDoctorPatient(record))
        assertEquals(before + 1, repository.getDoctorPatients().size)
        assertEquals(record, repository.getDoctorPatient(record.id))
        assertFalse(repository.addDoctorPatient(record.copy(id = "other-id")))
    }

    @Test
    fun deleteDoctorPatientRemovesRecord() {
        val repository = MockRepository()

        assertTrue(repository.deleteDoctorPatient("2403021728"))

        assertNull(repository.getDoctorPatient("2403021728"))
    }

    @Test
    fun getDoctorPatientsSearchesVisibleFields() {
        val repository = MockRepository()

        val byName = repository.getDoctorPatients(query = "陈建国")
        val byNumber = repository.getDoctorPatients(query = "230615")
        val bySummary = repository.getDoctorPatients(query = "重点跟踪")

        assertEquals("陈建国", byName.single().name)
        assertEquals("陈建国", byNumber.single().name)
        assertEquals("陈建国", bySummary.single().name)
    }

    @Test
    fun getDoctorPatientsSortsByAge() {
        val repository = MockRepository()

        val ascending = repository.getDoctorPatients(sortField = PatientSortField.Age, ascending = true)
        val descending = repository.getDoctorPatients(sortField = PatientSortField.Age, ascending = false)

        assertEquals(ascending.first().age, descending.last().age)
        assertTrue(ascending.first().age <= ascending.last().age)
        assertTrue(descending.first().age >= descending.last().age)
    }

    @Test
    fun initializationCanAdvanceAndReset() {
        val repository = MockRepository()
        val patientId = "2403021728"

        assertEquals(InitializationStep.ElectrodeConfig, repository.getInitializationStep(patientId))
        assertEquals(InitializationStep.BaselineDetection, repository.advanceInitialization(patientId))
        assertEquals(InitializationStep.FrequencyExtraction, repository.advanceInitialization(patientId))
        assertEquals(InitializationStep.Completed, repository.advanceInitialization(patientId))
        assertEquals(InitializationStep.Completed, repository.advanceInitialization(patientId))
        assertEquals(InitializationStep.ElectrodeConfig, repository.resetInitialization(patientId))
    }

    @Test
    fun updateDoctorPatientPersistsEditedProfile() {
        val repository = MockRepository()
        val original = repository.getDoctorPatient("2403021728")!!
        val updated = original.copy(age = original.age + 1, summary = "updated summary")

        assertTrue(repository.updateDoctorPatient(updated))

        assertEquals(updated.age, repository.getDoctorPatient(original.id)!!.age)
        assertEquals("updated summary", repository.getDoctorPatient(original.id)!!.summary)
    }

    @Test
    fun deviceConnectionCanToggle() {
        val repository = MockRepository()

        assertEquals(DeviceConnectionState.Disconnected, repository.disconnectDevice())
        assertEquals(DeviceConnectionState.Connected, repository.connectDevice())
    }

    @Test
    fun exportFilesCanCreateAndDelete() {
        val repository = MockRepository()
        val source = repository.getExportFiles().first()
        val before = repository.getExportFiles().size

        val created = repository.createExport(setOf(source.id), ExportFormat.PDF, ExportSettings())

        assertTrue(created.exported)
        assertEquals(before, repository.getExportFiles().size)
        assertEquals(0, repository.deleteExportFiles(setOf(created.id)))
    }

    @Test
    fun telehealthSessionCanSendMessageAndEnd() {
        val repository = MockRepository()
        val patientId = "2403021728"

        val session = repository.startTelehealth(patientId)
        val withMessage = repository.addTelehealthMessage(session.id, "请保持设备在线")!!
        val ended = repository.endTelehealth(session.id)!!

        assertTrue(withMessage.messages.any { it.content == "请保持设备在线" })
        assertFalse(ended.active)
    }

    @Test
    fun realtimeStateCanPauseRecordAndMarkEvent() {
        val repository = MockRepository()
        val patientId = "2403021728"

        val paused = repository.setRealtimePaused(patientId, true)
        repository.toggleRealtimeRecording(patientId)
        val saved = repository.toggleRealtimeRecording(patientId)
        val marked = repository.recordRealtimeEvent(patientId, "震颤加重")

        assertTrue(paused.paused)
        assertEquals(1, saved.recordedSegments)
        assertTrue(marked.eventMarkers.contains("震颤加重"))
    }

    @Test
    fun initializationBandsAndOptimizationSettingsPersist() {
        val repository = MockRepository()
        val patientId = "2403021728"
        val bands = FrequencyBands(staticBeta = "12-20 Hz", motionBeta = "18-28 Hz", gamma = "60-82 Hz")
        val settings = ParameterOptimizationSettings(optimizationRounds = 9)

        repository.saveInitializationFrequencyBands(patientId, bands)
        repository.saveParameterOptimizationSettings(patientId, settings)

        assertEquals(bands, repository.getInitializationFrequencyBands(patientId))
        assertEquals(9, repository.getParameterOptimizationSettings(patientId).optimizationRounds)
    }

    @Test
    fun electrodeConfigurationValidatesAndPersistsPerPatient() {
        val repository = MockRepository()
        val patientId = "2403021728"
        val workflow = repository.getInitializationWorkflow(patientId)
        val selection = ElectrodeSelection(leftPositive = 8, leftNegative = 6, rightPositive = 4, rightNegative = 2)

        val saved = repository.saveElectrodeConfiguration(
            patientId,
            selection,
            workflow.stimulationParameters
        )

        assertEquals(selection, saved.electrodeSelection)
        assertTrue(saved.stimulationParameters.all { it.isSafe() })
    }

    @Test(expected = IllegalArgumentException::class)
    fun electrodeConfigurationRejectsSamePolarityContact() {
        val repository = MockRepository()
        val patientId = "2403021728"
        repository.saveElectrodeConfiguration(
            patientId,
            ElectrodeSelection(leftPositive = 6, leftNegative = 6),
            repository.getInitializationWorkflow(patientId).stimulationParameters
        )
    }

    @Test
    fun baselineProgressAndPreviousStepKeepSavedData() {
        val repository = MockRepository()
        val patientId = "2403021728"
        val selection = ElectrodeSelection(leftPositive = 8, leftNegative = 5, rightPositive = 4, rightNegative = 1)
        repository.saveElectrodeConfiguration(
            patientId,
            selection,
            repository.getInitializationWorkflow(patientId).stimulationParameters
        )
        repository.advanceInitialization(patientId)
        repository.saveBaselineSamplingState(
            patientId,
            BaselineSamplingState(activeTask = 2, completedTasks = setOf(0, 1), elapsedSeconds = 60)
        )

        repository.previousInitialization(patientId)
        val workflow = repository.getInitializationWorkflow(patientId)

        assertEquals(InitializationStep.ElectrodeConfig, workflow.step)
        assertEquals(selection, workflow.electrodeSelection)
        assertEquals(setOf(0, 1), workflow.baseline.completedTasks)
    }

    @Test
    fun exportFileFiltersApplyDateRangeAndType() {
        val repository = MockRepository()

        val files = repository.getExportFiles(
            type = com.omnidapt.pd.data.ExportFileType.BrainSignal,
            dateFrom = "2026-07-24",
            dateTo = "2026-07-24"
        )

        assertTrue(files.isNotEmpty())
        assertTrue(files.all { it.generatedAt.startsWith("2026-07-24") })
        assertTrue(files.all { it.type == com.omnidapt.pd.data.ExportFileType.BrainSignal })
    }

    @Test
    fun selectedDoctorPatientGetsStableSimulatedDeviceFiles() {
        val repository = MockRepository()
        val patientId = "2403021728"

        val firstRead = repository.getExportFiles(patientId = patientId)
        val secondRead = repository.getExportFiles(patientId = patientId)

        assertEquals(5, firstRead.size)
        assertEquals(firstRead.map { it.id }, secondRead.map { it.id })
        assertTrue(firstRead.all { it.patientId == patientId && it.patientName == "王伟" })
        assertTrue(firstRead.all { it.fileName.startsWith("王伟_") })
    }

    @Test
    fun impedanceSeriesExposeLabeledDeviceInputContract() {
        val repository = MockRepository()

        val left = repository.getImpedanceSeries("2403021728", ImpedanceSide.Left, ImpedanceMode.Monopolar)
        val right = repository.getImpedanceSeries("2403021728", ImpedanceSide.Right, ImpedanceMode.Bipolar)

        assertEquals(listOf("C-5", "C-6", "C-7", "C-8"), left.map { it.contact })
        assertEquals(listOf("1-2", "2-3", "3-4"), right.map { it.contact })
    }
}
