package org.erlide.cover.ui.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTabGroup;
import org.eclipse.debug.ui.CommonTab;
import org.eclipse.debug.ui.EnvironmentTab;
import org.eclipse.debug.ui.ILaunchConfigurationDialog;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchConfigurationTabGroup;
import org.erlide.ui.launch.CodepathTab;
import org.erlide.ui.launch.DebugTab;
import org.erlide.ui.launch.ErlangMainTab;
import org.erlide.ui.launch.RuntimeTab;

/**
 * Tab group for cover launch configuration
 * 
 * @author Aleksandra Lipiec <aleksandra.lipiec@erlang.solutions.com>
 *
 */
public class CoverTabGroup extends AbstractLaunchConfigurationTabGroup {

	
	public void createTabs(ILaunchConfigurationDialog dialog, String mode) {
		final List<ILaunchConfigurationTab> tabs = new ArrayList<ILaunchConfigurationTab>(
				createMyTabs(dialog, mode));
		tabs.addAll(Arrays.asList(new ILaunchConfigurationTab[] {
				new EnvironmentTab(), new CommonTab() }));
		setTabs(tabs.toArray(new ILaunchConfigurationTab[0]));
		
	}
	
	private Collection<ILaunchConfigurationTab> createMyTabs(
			final ILaunchConfigurationDialog dialog, final String mode) {
		ILaunchConfigurationTab[] tabs;
		if (mode.equals("debug")) {
			tabs = new ILaunchConfigurationTab[] { new CoverMainTab(),
					new RuntimeTab(), new DebugTab(), new CodepathTab() };
		} else {
			tabs = new ILaunchConfigurationTab[] { new CoverMainTab(),
					new RuntimeTab(), new CodepathTab() };
		}
		return Arrays.asList(tabs);
	}

	

}
