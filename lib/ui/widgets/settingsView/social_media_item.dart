
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

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
