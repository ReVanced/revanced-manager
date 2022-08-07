import 'package:flutter/material.dart';
import 'package:revanced_manager/models/patch.dart';
import 'package:revanced_manager/ui/views/patches_selector/patches_selector_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/patch_item.dart';
import 'package:revanced_manager/ui/widgets/search_bar.dart';
import 'package:stacked/stacked.dart';

class PatchesSelectorView extends StatefulWidget {
  const PatchesSelectorView({Key? key}) : super(key: key);

  @override
  State<PatchesSelectorView> createState() => _PatchesSelectorViewState();
}

class _PatchesSelectorViewState extends State<PatchesSelectorView> {
  String query = '';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder.reactive(
      viewModelBuilder: () => PatchesSelectorViewModel(),
      builder: (context, PatchesSelectorViewModel model, child) => Scaffold(
        body: Container(
          margin: const EdgeInsets.fromLTRB(6.0, 26.0, 6.0, 0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.start,
            children: [
              Padding(
                padding: const EdgeInsets.only(top: 8.0, left: 8.0, right: 8.0),
                child: SearchBar(
                  hintText: "Search patches",
                  onQueryChanged: (searchQuery) {
                    setState(
                      () {
                        query = searchQuery;
                      },
                    );
                  },
                ),
              ),
              Expanded(
                child: FutureBuilder<List<Patch>?>(
                  future: model.getPatches(),
                  builder: (context, snapshot) {
                    if (snapshot.hasData) {
                      return ListView.builder(
                        itemCount: snapshot.data!.length,
                        itemBuilder: (context, index) {
                          if (query.isEmpty ||
                              query.length < 2) {
                            return PatchItem(
                              name: snapshot.data![index].simpleName,
                              version: snapshot.data![index].version,
                              description: snapshot.data![index].description,
                              isSelected: false,
                            );
                          }
                          else if(query.isNotEmpty &&
                              query.length >= 2 &&
                              snapshot.data![index].simpleName.toLowerCase().contains(query.toLowerCase())) {
                            return PatchItem(
                              name: snapshot.data![index].simpleName,
                              version: snapshot.data![index].version,
                              description: snapshot.data![index].description,
                              isSelected: false,
                            );
                          }
                          else {return Container();}

                        },
                      );
                    } else if (snapshot.hasError) {
                      return Text("${snapshot.error}");
                    } else {
                      return const Center(
                        child: CircularProgressIndicator(),
                      );
                    }
                  },
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
