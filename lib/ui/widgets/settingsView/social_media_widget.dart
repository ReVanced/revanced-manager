import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:url_launcher/url_launcher.dart';

class SocialMediaWidget extends StatelessWidget {
  final EdgeInsetsGeometry? padding;

  const SocialMediaWidget({
    Key? key,
    this.padding,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ExpandablePanel(
      theme: ExpandableThemeData(
        hasIcon: true,
        iconColor: Theme.of(context).iconTheme.color,
        iconPadding: const EdgeInsets.symmetric(vertical: 16.0),
        animationDuration: const Duration(milliseconds: 400),
      ),
      header: ListTile(
        contentPadding: padding ?? EdgeInsets.zero,
        title: I18nText(
          'socialMediaCard.widgetTitle',
          child: const Text(
            '',
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
          ),
        ),
        subtitle: I18nText('socialMediaCard.widgetSubtitle'),
      ),
      expanded: CustomCard(
        child: Column(
          children: const <Widget>[
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.github),
              title: Text('GitHub'),
              subtitle: Text('github.com/revanced'),
              url: 'https://github.com/revanced',
            ),
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.discord),
              title: Text('Discord'),
              subtitle: Text('discord.gg/revanced'),
              url: 'https://discord.gg/rF2YcEjcrT',
            ),
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.telegram),
              title: Text('Telegram'),
              subtitle: Text('t.me/app_revanced'),
              url: 'https://t.me/app_revanced',
            ),
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.reddit),
              title: Text('Reddit'),
              subtitle: Text('r/revancedapp'),
              url: 'https://reddit.com/r/revancedapp',
            ),
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.twitter),
              title: Text('Twitter'),
              subtitle: Text('@revancedapp'),
              url: 'https://twitter.com/revancedapp',
            ),
            SocialMediaItem(
              icon: FaIcon(FontAwesomeIcons.youtube),
              title: Text('YouTube'),
              subtitle: Text('youtube.com/revanced'),
              url: 'https://youtube.com/revanced',
            ),
          ],
        ),
      ),
      collapsed: Container(),
    );
  }
}

class SocialMediaItem extends StatelessWidget {
  final Widget? icon;
  final Widget title;
  final Widget? subtitle;
  final String? url;

  const SocialMediaItem({
    Key? key,
    this.icon,
    required this.title,
    this.subtitle,
    this.url,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return ListTile(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16.0)),
      contentPadding: EdgeInsets.zero,
      leading: SizedBox(
        width: 48.0,
        child: Center(
          child: icon,
        ),
      ),
      title: DefaultTextStyle(
        style: Theme.of(context).textTheme.bodyMedium!.copyWith(
              color: Theme.of(context).colorScheme.onSecondaryContainer,
            ),
        child: title,
      ),
      subtitle: subtitle != null
          ? DefaultTextStyle(
              style: Theme.of(context).textTheme.bodyMedium!.copyWith(
                    color: Theme.of(context).colorScheme.primary,
                  ),
              child: subtitle!,
            )
          : null,
      onTap: () => url != null
          ? launchUrl(
              Uri.parse(url!),
              mode: LaunchMode.externalApplication,
            )
          : null,
    );
  }
}
