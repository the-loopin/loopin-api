package com.loopin.api.architecture;

import com.loopin.api.common.architecturefixture.AllowedGroupsApiDependency;
import com.loopin.api.common.architecturefixture.ForbiddenGroupsRepositoryDependency;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GroupsApiBoundaryRuleTest {

    @Test
    void rejectsDirectRepositoryAccessFromCommon() {
        assertThrows(AssertionError.class, () ->
                ArchUnitModuleBoundaryTest.commonMustAccessGroupsThroughItsApi.check(
                        new ClassFileImporter().importClasses(ForbiddenGroupsRepositoryDependency.class)));
    }

    @Test
    void allowsGroupsApiAccessFromCommon() {
        assertDoesNotThrow(() ->
                ArchUnitModuleBoundaryTest.commonMustAccessGroupsThroughItsApi.check(
                        new ClassFileImporter().importClasses(AllowedGroupsApiDependency.class)));
    }
}
