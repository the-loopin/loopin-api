package com.loopin.api.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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
    public static final ArchRule eventsMustAccessGroupsThroughItsApi = noClasses()
            .that().resideInAPackage("com.loopin.api.events..")
            .and().resideOutsideOfPackage("..seed..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.loopin.api.groups.entity..", "com.loopin.api.groups.repository..")
            .because("Events must access Groups through com.loopin.api.groups.api.");

    @ArchTest
    public static final ArchRule commonMustAccessGroupsThroughItsApi = noClasses()
            .that().resideInAPackage("com.loopin.api.common..")
            .should().dependOnClassesThat(new DescribedPredicate<>("reside in Groups outside its api package") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return javaClass.getPackageName().startsWith("com.loopin.api.groups")
                            && !javaClass.getPackageName().startsWith("com.loopin.api.groups.api");
                }
            })
            .because("Common infrastructure must use the Groups application API, never Groups internals.");

    @ArchTest
    public static final ArchRule publicGroupsApiMustNotExposeGroupsPersistenceTypes = noClasses()
            .that().resideInAPackage("com.loopin.api.groups.api..")
            .and().arePublic()
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.loopin.api.groups.entity..", "com.loopin.api.groups.repository..")
            .because("Groups API contracts must not expose Groups entities, repositories, or internal database identifiers.");

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
