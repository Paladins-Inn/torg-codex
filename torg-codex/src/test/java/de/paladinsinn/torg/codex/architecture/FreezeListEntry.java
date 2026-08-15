package de.paladinsinn.torg.codex.architecture;

record FreezeListEntry(
        String id,
        String module,
        String violatingClassOrDependency,
        String violatedRule,
        String rationale,
        String baselineTask,
        String plannedRemovalPhase,
        String status) {

    boolean matches(String violation) {
        return !violatingClassOrDependency.isBlank() && violation.contains(violatingClassOrDependency);
    }
}
