import 'package:flutter/material.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/models/detected_plugin.dart';

class DetectedPluginItem extends StatelessWidget {
  final DetectedPlugin plugin;
  final VoidCallback? onUninstall;
  final IconData categoryIcon;
  final Color categoryColor;

  const DetectedPluginItem({
    super.key,
    required this.plugin,
    this.onUninstall,
    required this.categoryIcon,
    required this.categoryColor,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;
    
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 4.0),
      child: ListTile(
        leading: plugin.icon != null
            ? ClipRRect(
                borderRadius: BorderRadius.circular(8.0),
                child: Image.memory(
                  plugin.icon!,
                  width: 40,
                  height: 40,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) => Icon(
                    categoryIcon,
                    color: categoryColor,
                    size: 40,
                  ),
                ),
              )
            : Container(
                width: 40,
                height: 40,
                decoration: BoxDecoration(
                  color: categoryColor.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(8.0),
                ),
                child: Icon(
                  categoryIcon,
                  color: categoryColor,
                  size: 24,
                ),
              ),
        title: Text(
          plugin.appName,
          style: Theme.of(context).textTheme.titleMedium?.copyWith(
            fontWeight: FontWeight.w600,
          ),
        ),
        subtitle: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const SizedBox(height: 4),
            Text(
              plugin.packageName,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant,
                fontFamily: 'monospace',
              ),
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: categoryColor.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(
                      color: categoryColor.withOpacity(0.3),
                      width: 1,
                    ),
                  ),
                  child: Text(
                    plugin.category.displayName,
                    style: Theme.of(context).textTheme.labelSmall?.copyWith(
                      color: categoryColor,
                      fontWeight: FontWeight.w600,
                    ),
                  ),
                ),
                const SizedBox(width: 8),
                if (!plugin.hasLauncherIcon)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: colorScheme.secondary.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      t.settingsView.hiddenFromLauncher,
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: colorScheme.secondary,
                      ),
                    ),
                  ),
                if (plugin.isSystemApp)
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: colorScheme.outline.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      t.settingsView.systemApp,
                      style: Theme.of(context).textTheme.labelSmall?.copyWith(
                        color: colorScheme.outline,
                      ),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              'v${plugin.versionName}',
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: colorScheme.onSurfaceVariant,
              ),
            ),
          ],
        ),
        trailing: plugin.isUninstallable
            ? IconButton(
                onPressed: onUninstall,
                icon: Icon(
                  Icons.delete_outline,
                  color: colorScheme.error,
                ),
                tooltip: t.appInfoView.uninstallButton,
              )
            : Tooltip(
                message: plugin.isSystemApp 
                    ? t.settingsView.uninstallNotAvailable
                    : 'Cannot uninstall this app',
                child: Icon(
                  Icons.block,
                  color: colorScheme.outline,
                ),
              ),
        contentPadding: const EdgeInsets.symmetric(horizontal: 16.0, vertical: 8.0),
      ),
    );
  }
}