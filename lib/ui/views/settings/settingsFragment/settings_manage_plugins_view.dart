import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/detected_plugin.dart';
import 'package:revanced_manager/ui/views/settings/settingsFragment/settings_manage_plugins_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/settingsView/detected_plugin_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class SettingsManagePluginsView extends StatelessWidget {
  const SettingsManagePluginsView({super.key});

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<SettingsManagePluginsViewModel>.reactive(
      viewModelBuilder: () => SettingsManagePluginsViewModel(),
      onViewModelReady: (model) => model.initialize(),
      builder: (context, model, child) => Scaffold(
        body: CustomScrollView(
          slivers: [
            CustomSliverAppBar(
              title: Text(
                t.settingsView.managePluginsLabel,
                style: GoogleFonts.inter(
                  color: Theme.of(context).textTheme.titleLarge!.color,
                ),
              ),
              actions: [
                IconButton(
                  onPressed: model.isLoading ? null : model.detectPlugins,
                  icon: model.isLoading 
                      ? SizedBox(
                          width: 20,
                          height: 20,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : Icon(Icons.refresh),
                  tooltip: t.settingsView.refreshPlugins,
                ),
              ],
            ),
            SliverToBoxAdapter(
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      t.settingsView.managePluginsHint,
                      style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                        color: Theme.of(context).colorScheme.onSurfaceVariant,
                      ),
                    ),
                    const SizedBox(height: 16),
                    if (model.isLoading)
                      Center(
                        child: Column(
                          children: [
                            const CircularProgressIndicator(),
                            const SizedBox(height: 16),
                            Text(t.settingsView.detectedPluginsLabel),
                          ],
                        ),
                      )
                    else ...[
                      Row(
                        children: [
                          Icon(
                            Icons.info_outline,
                            size: 20,
                            color: Theme.of(context).colorScheme.primary,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            model.detectedPlugins.isEmpty
                                ? t.settingsView.noPluginsDetected
                                : t.settingsView.pluginsDetectedMessage(
                                    count: model.uninstallablePlugins.length,
                                  ),
                            style: Theme.of(context).textTheme.titleSmall?.copyWith(
                              color: Theme.of(context).colorScheme.primary,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                    ],
                  ],
                ),
              ),
            ),
            if (!model.isLoading && model.detectedPlugins.isNotEmpty)
              SliverList(
                delegate: SliverChildBuilderDelegate(
                  (context, index) {
                    final plugin = model.detectedPlugins[index];
                    return DetectedPluginItem(
                      plugin: plugin,
                      onUninstall: plugin.isUninstallable
                          ? () => model.uninstallPlugin(plugin, context)
                          : null,
                      categoryIcon: model.getCategoryIcon(plugin.category),
                      categoryColor: model.getCategoryColor(plugin.category, context),
                    );
                  },
                  childCount: model.detectedPlugins.length,
                ),
              ),
            if (!model.isLoading && model.detectedPlugins.isEmpty)
              SliverFillRemaining(
                child: Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(
                        Icons.extension_off,
                        size: 64,
                        color: Theme.of(context).colorScheme.outline,
                      ),
                      const SizedBox(height: 16),
                      Text(
                        t.settingsView.noPluginsDetected,
                        style: Theme.of(context).textTheme.titleMedium?.copyWith(
                          color: Theme.of(context).colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        t.settingsView.noPluginsFoundDescription,
                        style: Theme.of(context).textTheme.bodyMedium?.copyWith(
                          color: Theme.of(context).colorScheme.outline,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ],
                  ),
                ),
              ),
          ],
        ),
      ),
    );
  }
}