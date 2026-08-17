"""Add four-state initialization workflow and segment quality records."""

from alembic import op
import sqlalchemy as sa

revision = "0002_initialization"
down_revision = "0001_initial"
branch_labels = None
depends_on = None


initialization_status = sa.Enum(
    "DRAFT",
    "CONFIGURING",
    "CAPTURING",
    "ANALYZING",
    "REVIEW",
    "APPROVED",
    "FAILED",
    "CANCELLED",
    name="initializationstatus",
)


def upgrade() -> None:
    op.execute("UPDATE devices SET protocol_version = 2 WHERE simulated = true")
    # 0001 uses Base.metadata.create_all and therefore reflects the currently
    # installed model set on a brand-new database. Existing 0001 deployments do
    # not have these tables, while fresh databases already do.
    if "initialization_runs" in sa.inspect(op.get_bind()).get_table_names():
        return
    op.create_table(
        "initialization_runs",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("patient_id", sa.String(length=36), nullable=False),
        sa.Column("device_id", sa.String(length=36), nullable=False),
        sa.Column("created_by", sa.String(length=36), nullable=False),
        sa.Column("mode", sa.String(length=20), nullable=False),
        sa.Column("status", initialization_status, nullable=False),
        sa.Column("current_state", sa.String(length=40), nullable=True),
        sa.Column("settle_seconds", sa.Integer(), nullable=False),
        sa.Column("capture_seconds", sa.Integer(), nullable=False),
        sa.Column("electrode_config", sa.JSON(), nullable=False),
        sa.Column("quality_summary", sa.JSON(), nullable=False),
        sa.Column("frequency_results", sa.JSON(), nullable=False),
        sa.Column("model_version_id", sa.String(length=36), nullable=True),
        sa.Column("error", sa.Text(), nullable=True),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
        sa.Column("approved_at", sa.DateTime(timezone=True), nullable=True),
        sa.ForeignKeyConstraint(["created_by"], ["users.id"]),
        sa.ForeignKeyConstraint(["device_id"], ["devices.id"]),
        sa.ForeignKeyConstraint(["model_version_id"], ["model_versions.id"]),
        sa.ForeignKeyConstraint(["patient_id"], ["patients.id"]),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index("ix_initialization_runs_patient_id", "initialization_runs", ["patient_id"])
    op.create_index("ix_initialization_runs_device_id", "initialization_runs", ["device_id"])
    op.create_index("ix_initialization_runs_created_by", "initialization_runs", ["created_by"])
    op.create_index("ix_initialization_runs_status", "initialization_runs", ["status"])
    op.create_index(
        "ix_initialization_runs_model_version_id",
        "initialization_runs",
        ["model_version_id"],
    )
    op.create_table(
        "initialization_segments",
        sa.Column("id", sa.String(length=36), nullable=False),
        sa.Column("initialization_id", sa.String(length=36), nullable=False),
        sa.Column("lfp_session_id", sa.String(length=36), nullable=False),
        sa.Column("state_label", sa.String(length=40), nullable=False),
        sa.Column("order_index", sa.Integer(), nullable=False),
        sa.Column("sample_count", sa.Integer(), nullable=False),
        sa.Column("received_frames", sa.Integer(), nullable=False),
        sa.Column("packet_loss_count", sa.Integer(), nullable=False),
        sa.Column("crc_error_count", sa.Integer(), nullable=False),
        sa.Column("saturated_sample_count", sa.Integer(), nullable=False),
        sa.Column("impedance", sa.JSON(), nullable=False),
        sa.Column("quality", sa.JSON(), nullable=False),
        sa.Column("accepted", sa.Boolean(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
        sa.ForeignKeyConstraint(
            ["initialization_id"],
            ["initialization_runs.id"],
            ondelete="CASCADE",
        ),
        sa.ForeignKeyConstraint(["lfp_session_id"], ["lfp_sessions.id"]),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("initialization_id", "state_label"),
        sa.UniqueConstraint("lfp_session_id"),
    )
    op.create_index(
        "ix_initialization_segments_initialization_id",
        "initialization_segments",
        ["initialization_id"],
    )
    op.create_index(
        "ix_initialization_segments_lfp_session_id",
        "initialization_segments",
        ["lfp_session_id"],
    )
    op.create_index(
        "ix_initialization_segments_state_label",
        "initialization_segments",
        ["state_label"],
    )


def downgrade() -> None:
    op.drop_table("initialization_segments")
    op.drop_table("initialization_runs")
    initialization_status.drop(op.get_bind(), checkfirst=True)
