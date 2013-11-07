package org.erlide.ui.wizards

import java.util.Map
import org.erlide.engine.model.builder.BuilderConfig
import org.erlide.engine.model.root.ErlangProjectProperties

class ProjectPreferencesWizardPageFactory {

    val static Map<BuilderConfig, Class<? extends ProjectPreferencesWizardPage>> PAGES = #{
        BuilderConfig.INTERNAL -> InternalProjectPreferencesWizardPage,
        BuilderConfig.EMAKE -> EmakeProjectPreferencesWizardPage,
        BuilderConfig.REBAR -> RebarProjectPreferencesWizardPage
    }

    static def ProjectPreferencesWizardPage create(BuilderConfig builder, ErlangProjectProperties info) {
        val clazz = PAGES.get(builder)
        clazz.constructors.get(0).newInstance("buildPage", info) as ProjectPreferencesWizardPage
    }

}
