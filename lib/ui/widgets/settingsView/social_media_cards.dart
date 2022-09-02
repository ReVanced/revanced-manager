import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:revanced_manager/constants.dart';
import 'package:revanced_manager/theme.dart';
import 'package:url_launcher/url_launcher.dart';

class SocialMediaCard extends StatelessWidget {
  const SocialMediaCard({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 16),
      child: ExpandablePanel(
        theme: ExpandableThemeData(
          hasIcon: true,
          iconColor: Theme.of(context).iconTheme.color,
          animationDuration: const Duration(milliseconds: 450),
        ),
        header: SizedBox(
          width: double.infinity,
          child: ListTile(
            contentPadding: EdgeInsets.zero,
            title: I18nText(
              'socialMediaCard.widgetTitle',
              child: Text('', style: kSettingItemTextStyle),
            ),
          ),
        ),
        expanded: Card(
          color: isDark
              ? Theme.of(context).colorScheme.primary
              : Theme.of(context).navigationBarTheme.backgroundColor!,
          child: Column(
            children: <Widget>[
              ListTile(
                leading: const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: FaIcon(FontAwesomeIcons.github),
                ),
                title: const Text('GitHub'),
                subtitle: const Text('github.com/revanced'),
                onTap: () => launchUrl(
                  Uri.parse('https://github.com/revanced'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
              ListTile(
                leading: Padding(
                  padding: const EdgeInsets.all(8.0).copyWith(left: 5),
                  child: const FaIcon(FontAwesomeIcons.discord),
                ),
                title: const Text('Discord'),
                subtitle: const Text('discord.gg/revanced'),
                onTap: () => launchUrl(
                  Uri.parse('https://discord.gg/rF2YcEjcrT'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
              ListTile(
                leading: const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: FaIcon(FontAwesomeIcons.telegram),
                ),
                title: const Text('Telegram'),
                subtitle: const Text('t.me/app_revanced'),
                onTap: () => launchUrl(
                  Uri.parse('https://t.me/app_revanced'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
              ListTile(
                leading: const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: FaIcon(FontAwesomeIcons.reddit),
                ),
                title: const Text('Reddit'),
                subtitle: const Text('r/revancedapp'),
                onTap: () => launchUrl(
                  Uri.parse('https://reddit.com/r/revancedapp'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
              ListTile(
                leading: const Padding(
                  padding: EdgeInsets.all(8.0),
                  child: FaIcon(FontAwesomeIcons.twitter),
                ),
                title: const Text('Twitter'),
                subtitle: const Text('@revancedapp'),
                onTap: () => launchUrl(
                  Uri.parse('https://twitter.com/revancedapp'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
              ListTile(
                leading: const Padding(
                    padding: EdgeInsets.all(8.0),
                    child: FaIcon(FontAwesomeIcons.youtube)),
                title: const Text('YouTube'),
                subtitle: const Text('youtube.com/revanced'),
                onTap: () => launchUrl(
                  Uri.parse('https://youtube.com/revanced'),
                  mode: LaunchMode.externalApplication,
                ),
              ),
            ],
          ),
        ),
        collapsed: I18nText(
          'socialMediaCard.widgetSubtitle',
          child: Text(
            '',
            style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                  color: isDark ? Colors.grey[400] : Colors.grey[600],
                ),
          ),
        ),
      ),
    );
  }
}
