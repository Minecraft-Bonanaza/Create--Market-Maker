package com.bng.marketcoordination.economy;

/**
 * v0.4 metadata-only procurement pool stub. No virtual delivery or teleportation.
 */
public final class ProcurementPool {
    private long publicWorksBudgetSpurs;
    private long militaryBudgetSpurs;
    private String notes = "";

    public long publicWorksBudgetSpurs() { return publicWorksBudgetSpurs; }
    public long militaryBudgetSpurs() { return militaryBudgetSpurs; }
    public String notes() { return notes; }

    public void setPublicWorksBudgetSpurs(long publicWorksBudgetSpurs) {
        this.publicWorksBudgetSpurs = Math.max(0L, publicWorksBudgetSpurs);
    }

    public void setMilitaryBudgetSpurs(long militaryBudgetSpurs) {
        this.militaryBudgetSpurs = Math.max(0L, militaryBudgetSpurs);
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
    }
}
