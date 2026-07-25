package com.omnidapt.pd

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.omnidapt.pd.data.MockRepository
import com.omnidapt.pd.ui.theme.OminidaptTheme
import org.junit.Rule
import org.junit.Test

class DoctorWorkflowUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun doctorLoginShowsCompleteNavigationAndPatientList() {
        composeRule.setContent {
            OminidaptTheme {
                OminidaptApp(MockRepository())
            }
        }

        composeRule.onNodeWithText("医生").performClick()
        composeRule.onAllNodesWithText("登录")[1].performClick()

        composeRule.onNodeWithText("患者列表").assertExists()
        composeRule.onNodeWithText("文件导出").assertExists()
        composeRule.onNodeWithText("个人设置").assertExists()
        composeRule.onNodeWithText("初始化与参数调整").assertExists()
        composeRule.onNodeWithText("实时观测").assertExists()
        composeRule
            .onNodeWithText("长按患者行可设为当前患者；受限功能需要先选中患者。")
            .assertExists()
    }

    @Test
    fun patientLoginNavigatesAcrossPrimaryTabs() {
        composeRule.setContent {
            OminidaptTheme {
                OminidaptApp(MockRepository())
            }
        }

        composeRule.onNodeWithText("患者").performClick()
        composeRule.onAllNodesWithText("登录")[1].performClick()

        composeRule.onNodeWithText("首页").assertExists()
        composeRule.onNodeWithText("数据报告").performClick()
        composeRule.onNodeWithText("开始调参").assertExists()
        composeRule.onNodeWithText("远程诊疗").performClick()
        composeRule.onNodeWithText("联系医生").assertExists()
        composeRule.onNodeWithText("个人信息").performClick()
        composeRule.onNodeWithText("查看个人资料、设备、记录和账户设置").assertExists()
    }

    @Test
    fun doctorTopLevelPagesRemainReachableAfterMotionUpgrade() {
        composeRule.setContent {
            OminidaptTheme {
                OminidaptApp(MockRepository())
            }
        }

        composeRule.onNodeWithText("医生").performClick()
        composeRule.onAllNodesWithText("登录")[1].performClick()

        composeRule.onNodeWithText("文件导出").performClick()
        composeRule.onNodeWithText("文件导出").assertExists()
        composeRule.onNodeWithText("个人设置").performClick()
        composeRule.onNodeWithText("个人设置").assertExists()
    }
}
