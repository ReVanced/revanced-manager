import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_i18n/flutter_i18n.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/services/app_info_api.dart';
import 'package:revanced_manager/services/manager_api.dart';
import 'package:revanced_manager/ui/views/settings/settings_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_card.dart';

class NotInstalledAppItem extends StatefulWidget {
  const NotInstalledAppItem({
    Key? key,
    required this.name,
    required this.patchesCount,
    required this.suggestedVersion,
    this.onTap,
  }) : super(key: key);
  final String name;
  final int patchesCount;
  final String suggestedVersion;
  final Function()? onTap;

  @override
  State<NotInstalledAppItem> createState() => _NotInstalledAppItem();
}

List<Map<String, dynamic>> _packageDetails = [];
final ManagerAPI _managerAPI = locator<ManagerAPI>();
final _settingsViewModel = SettingsViewModel();
final bool fetch = _settingsViewModel.isFetchAppInfoEnabled();
final List<String> _sources = _managerAPI.getAppInfoSources();

class _NotInstalledAppItem extends State<NotInstalledAppItem> {
  @override
  void initState() {
    super.initState();
    if(fetch){
      _packageDetails = _managerAPI.getAppInfo();
      if (!_packageDetails.toString().contains('${widget.name}}')) {
        getPackageDetails();
        _managerAPI.setAppInfo(_packageDetails);
      }
    }
  }

  Future getPackageDetails() async {
    final packageInfo = await getAppInfo(widget.name, _sources);
    if (!_packageDetails.contains(packageInfo)) {
      _packageDetails.add(packageInfo);
      setState(() {});
    }
  }

  @override
  Widget build(BuildContext context) {
    final int index = fetch
        ? (_packageDetails.toString().contains('${widget.name}}'))
            ? _packageDetails.indexOf(
                _packageDetails
                    .firstWhere((element) => element['pkgName'] == widget.name),
              )
            : -1
        : -1;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4.0),
      child: CustomCard(
        onTap: widget.onTap,
        child: Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: <Widget>[
            Container(
              height: 48,
              width: 48,
              padding: const EdgeInsets.symmetric(vertical: 4.0),
              alignment: Alignment.center,
              child: (index != -1)
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: CachedNetworkImage(
                          imageUrl: _packageDetails[index]['image'],
                          progressIndicatorBuilder: (context, url, progress) =>
                              SizedBox(
                                height: 48,
                                width: 48,
                                child: Center(
                                  child: CircularProgressIndicator(
                                    value: progress.progress,
                                  ),
                                ),
                              ),
                      ),
                    )
                  : const CircleAvatar(
                      backgroundColor: Colors.transparent,
                      child: Icon(
                        Icons.square_rounded,
                        color: Colors.grey,
                        size: 44,
                      ),
                    ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  if (index != -1)
                    Padding(
                      padding: const EdgeInsets.only(bottom: 4.0),
                      child: Text(
                        _packageDetails[index]['name'],
                        maxLines: 2,
                        overflow: TextOverflow.visible,
                        style: const TextStyle(
                          fontSize: 16,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ),
                  Text(widget.name),
                  I18nText(
                    'appSelectorCard.notInstalled',
                    child: Text(
                      '',
                      style: TextStyle(
                        color: Theme.of(context).textTheme.titleLarge!.color,
                      ),
                    ),
                  ),
                  Row(
                    children: [
                      I18nText(
                        'suggested',
                        translationParams: {
                          'version': widget.suggestedVersion.isEmpty
                              ? FlutterI18n.translate(
                                  context,
                                  'appSelectorCard.allVersions',
                                )
                              : 'v${widget.suggestedVersion}',
                        },
                      ),
                      const SizedBox(width: 4),
                      Text(
                        widget.patchesCount == 1
                            ? '• ${widget.patchesCount} patch'
                            : '• ${widget.patchesCount} patches',
                        style: TextStyle(
                          color: Theme.of(context).colorScheme.secondary,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}
