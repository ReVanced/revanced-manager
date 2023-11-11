import 'package:expandable/expandable.dart';
import 'package:flutter/material.dart';
import 'package:font_awesome_flutter/font_awesome_flutter.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/widgets/settingsView/social_media_item.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_icon.dart';

class SocialMediaWidget extends StatelessWidget {
  const SocialMediaWidget({
    super.key,
    this.padding,
  });
  final EdgeInsetsGeometry? padding;

  @override
  Widget build(BuildContext context) {
    return ExpandablePanel(
      theme: ExpandableThemeData(
        hasIcon: true,
        iconColor: Theme.of(context).iconTheme.color,
        iconPadding: const EdgeInsets.symmetric(vertical: 16.0)
            .add(padding ?? EdgeInsets.zero)
            .resolve(Directionality.of(context)),
        animationDuration: const Duration(milliseconds: 400),
      ),
      header: ListTile(
        contentPadding: padding ?? EdgeInsets.zero,
        title: Text(
          t.socialMediaCard.widgetTitle,
          style: const TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.w500,
          ),
        ),
        subtitle: Text(t.socialMediaCard.widgetSubtitle),
      ),
      expanded: Padding(
        padding: padding ?? EdgeInsets.zero,
        child: const CustomCard(
          child: Column(
            children: <Widget>[
              SocialMediaItem(
                icon: Icon(CustomIcon.revancedIcon),
                title: Text('Website'),
                subtitle: Text('revanced.app'),
                url: 'https://revanced.app',
              ),
              SocialMediaItem(
                icon: FaIcon(FontAwesomeIcons.github),
                title: Text('GitHub'),
                subtitle: Text('github.com/ReVanced'),
                url: 'https://github.com/ReVanced',
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
                icon: FaIcon(FontAwesomeIcons.xTwitter),
                title: Text('X'),
                subtitle: Text('@revancedapp'),
                url: 'https://x.com/revancedapp',
              ),
              SocialMediaItem(
                icon: FaIcon(FontAwesomeIcons.youtube),
                title: Text('YouTube'),
                subtitle: Text('youtube.com/@revanced'),
                url: 'https://youtube.com/@revanced',
              ),
            ],
          ),
        ),
      ),
      collapsed: const SizedBox(),
    );
  }
}
