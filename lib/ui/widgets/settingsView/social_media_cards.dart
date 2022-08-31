import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/widgets/I18nText.dart';
import 'package:revanced_manager/constants.dart';
import 'package:url_launcher/url_launcher.dart';

class SocialMediaCards extends StatelessWidget {
  const SocialMediaCards({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ExpandablePanel(
      theme: ExpandableThemeData(
        hasIcon: true,
        iconColor: Theme.of(context).iconTheme.color,
        animationDuration: const Duration(milliseconds: 450),
      ),
      header: SizedBox(
        width: double.infinity,
        child: ListTile(
          title: I18nText(
            'socialMediaCards.widgetTitle',
            child: Text('', style: kSettingItemTextStyle),
          ),
        ),
      ),
      expanded: Card(
        color: Theme.of(context).backgroundColor,
        child: Column(
          children: [
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Image.asset(
                  'assets/images/github.png',
                  height: 24,
                  width: 24,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('GitHub'),
              subtitle: const Text('github.com/revanced'),
              onTap: () => launchUrl(
                Uri.parse('https://github.com/revanced'),
                mode: LaunchMode.externalApplication,
              ),
            ),
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Icon(
                  Icons.discord,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('Discord'),
              subtitle: const Text('discord.gg/revanced'),
              onTap: () => launchUrl(
                Uri.parse('https://discord.gg/3E2pTWR4Yd'),
                mode: LaunchMode.externalApplication,
              ),
            ),
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Icon(
                  Icons.telegram,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('Telegram'),
              subtitle: const Text('t.me/app_revanced'),
              onTap: () => launchUrl(
                Uri.parse('https://t.me/app_revanced'),
                mode: LaunchMode.externalApplication,
              ),
            ),
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Icon(
                  Icons.reddit,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('Reddit'),
              subtitle: const Text('r/revancedapp'),
              onTap: () => launchUrl(
                Uri.parse('https://reddit.com/r/revancedapp'),
                mode: LaunchMode.externalApplication,
              ),
            ),
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Image.asset(
                  'assets/images/twitter.png',
                  height: 24,
                  width: 24,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('Twitter'),
              subtitle: const Text('@revancedapp'),
              onTap: () => launchUrl(
                Uri.parse('https://twitter.com/@revancedapp'),
                mode: LaunchMode.externalApplication,
              ),
            ),
            ListTile(
              contentPadding:
                  const EdgeInsets.symmetric(horizontal: 16).copyWith(top: 0),
              leading: Padding(
                padding: const EdgeInsets.all(8.0),
                child: Image.asset(
                  'assets/images/youtube.png',
                  height: 24,
                  width: 24,
                  color: Theme.of(context).iconTheme.color,
                ),
              ),
              title: const Text('YouTube'),
              subtitle: const Text('youtube.com/revanced'),
              onTap: () => launchUrl(
                Uri.parse(
                    'https://www.youtube.com/channel/UCLktAUh5Gza9zAJBStwxNdw'),
                mode: LaunchMode.externalApplication,
              ),
            ),
          ],
        ),
      ),
      collapsed: const Text(''),
    );
  }
}
