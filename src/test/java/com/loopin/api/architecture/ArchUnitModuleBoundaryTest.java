package com.loopin.api.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;

@AnalyzeClasses(packages = "com.loopin.api", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchUnitModuleBoundaryTest {

    @ArchTest
    public static final ArchRule controllersMustNotDependOnRepositories = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .because("Controllers must delegate to handlers/services, not access repositories directly.");

    @ArchTest
    public static final ArchRule eventsMustNotDependOnForeignRepositories = noClasses()
            .that().resideInAPackage("com.loopin.api.events..")
            .and().resideOutsideOfPackage("..seed..")
            .should().dependOnClassesThat().resideInAPackage("com.loopin.api.groups.repository..")
            .because("Modules must not import another module's repository package in application handlers.");

    @ArchTest
    public static final ArchRule groupsMustNotDependOnNotificationsService = noClasses()
            .that().resideInAPackage("com.loopin.api.groups..")
            .should().dependOnClassesThat().haveSimpleName("NotificationService")
            .because("Cross-module notification writes must use the api package boundary (NotificationWriter).");

    @ArchTest
    public static final ArchRule queryHandlersMustBeReadOnly = classes()
            .that().haveSimpleNameEndingWith("Handler")
            .and().resideInAnyPackage("..get*..", "..list*..")
            .should().beAnnotatedWith("org.springframework.transaction.annotation.Transactional")
            .allowEmptyShould(true)
            .because("Query handlers must declare read-only transactions.");
}
