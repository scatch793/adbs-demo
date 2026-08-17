"""Dual inference, analysis progress, and optimization collaboration state."""

from alembic import op
import sqlalchemy as sa

revision = "0003_realtime_workflow"
down_revision = "0002_initialization"
branch_labels = None
depends_on = None


def _columns(table: str) -> set[str]:
    return {column["name"] for column in sa.inspect(op.get_bind()).get_columns(table)}


def upgrade() -> None:
    op.execute("UPDATE devices SET protocol_version = 3 WHERE simulated = true")
    if "analysis_stage" not in _columns("initialization_runs"):
        op.add_column(
            "initialization_runs",
            sa.Column("analysis_stage", sa.String(length=40), nullable=False, server_default="idle"),
        )
        op.add_column(
            "initialization_runs",
            sa.Column("progress_percent", sa.Integer(), nullable=False, server_default="0"),
        )
    task_columns = _columns("optimization_tasks")
    additions = (
        ("current_round", sa.Integer(), "1"),
        ("observation_seconds", sa.Integer(), "30"),
        ("eligible_at", sa.DateTime(timezone=True), None),
        ("current_parameters", sa.JSON(), "{}"),
        ("best_parameters", sa.JSON(), "{}"),
        ("excluded_currents", sa.JSON(), "[]"),
        ("completed_at", sa.DateTime(timezone=True), None),
    )
    for name, type_, default in additions:
        if name not in task_columns:
            op.add_column(
                "optimization_tasks",
                sa.Column(
                    name,
                    type_,
                    nullable=default is not None,
                    server_default=default,
                ),
            )
    feedback_columns = _columns("feedback_responses")
    if "round_index" not in feedback_columns:
        op.add_column(
            "feedback_responses",
            sa.Column("round_index", sa.Integer(), nullable=False, server_default="1"),
        )
        op.add_column(
            "feedback_responses",
            sa.Column("submitted_by", sa.String(length=36), nullable=True),
        )
        op.create_foreign_key(
            "fk_feedback_responses_submitted_by_users",
            "feedback_responses",
            "users",
            ["submitted_by"],
            ["id"],
        )
    proposal_columns = _columns("parameter_proposals")
    if "round_index" not in proposal_columns:
        op.add_column(
            "parameter_proposals",
            sa.Column("round_index", sa.Integer(), nullable=False, server_default="1"),
        )
        op.add_column(
            "parameter_proposals",
            sa.Column("acquisition", sa.JSON(), nullable=False, server_default="{}"),
        )


def downgrade() -> None:
    for column in ("acquisition", "round_index"):
        op.drop_column("parameter_proposals", column)
    op.drop_constraint(
        "fk_feedback_responses_submitted_by_users",
        "feedback_responses",
        type_="foreignkey",
    )
    for column in ("submitted_by", "round_index"):
        op.drop_column("feedback_responses", column)
    for column in (
        "completed_at",
        "excluded_currents",
        "best_parameters",
        "current_parameters",
        "eligible_at",
        "observation_seconds",
        "current_round",
    ):
        op.drop_column("optimization_tasks", column)
    op.drop_column("initialization_runs", "progress_percent")
    op.drop_column("initialization_runs", "analysis_stage")
