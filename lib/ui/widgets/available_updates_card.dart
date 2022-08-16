import 'package:flutter/material.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/models/patched_application.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/application_item.dart';

class AvailableUpdatesCard extends StatelessWidget {
  const AvailableUpdatesCard({
    Key? key,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisAlignment: MainAxisAlignment.start,
      children: [
        FutureBuilder<List<PatchedApplication>>(
          future: locator<HomeViewModel>().getPatchedApps(true),
          builder: (context, snapshot) =>
              snapshot.hasData && snapshot.data!.length > 1
                  ? ListView.builder(
                      itemBuilder: (context, index) => ApplicationItem(
                        icon: snapshot.data![index].icon,
                        name: snapshot.data![index].name,
                        patchDate: snapshot.data![index].patchDate,
                        isUpdatableApp: true,
                        onPressed: () => {},
                      ),
                    )
                  : Container(),
        ),
      ],
    );
  }
}
