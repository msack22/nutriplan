package com.nutriplan.pricing.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * ArchUnit: las reglas de arquitectura como TESTS. Si alguien hace que el dominio dependa de un
 * controller, el build falla. La arquitectura deja de ser un dibujo y pasa a ser verificable.
 */
@AnalyzeClasses(packages = "com.nutriplan.pricing", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule layers = layeredArchitecture().consideringOnlyDependenciesInLayers()
            .layer("Web").definedBy("..web..")
            .layer("Application").definedBy("..application..")
            .layer("Domain").definedBy("..domain..")
            .layer("Infrastructure").definedBy("..infrastructure..")
            .whereLayer("Web").mayNotBeAccessedByAnyLayer()
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Web", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Web", "Infrastructure");

    @ArchTest
    static final ArchRule domainDoesNotUseHttpClients = noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("org.springframework.web..", "..infrastructure..");

    @ArchTest
    static final ArchRule controllersDoNotTouchRepositories = noClasses().that().resideInAPackage("..web..")
            .should().dependOnClassesThat().resideInAPackage("..domain.repository..");
}
