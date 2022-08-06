import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:revanced_manager_flutter/services/github_api.dart';
import 'package:revanced_manager_flutter/constants.dart';
import 'package:revanced_manager_flutter/ui/widgets/patch_text_button.dart';

class LatestCommitCard extends StatefulWidget {
  const LatestCommitCard({Key? key}) : super(key: key);

  @override
  State<LatestCommitCard> createState() => _LatestCommitCardState();
}

class _LatestCommitCardState extends State<LatestCommitCard> {
  GithubAPI githubAPI = GithubAPI();
  String lastPatcherCommit = "Loading...";
  String lastManagerCommit = "Loading...";

  void latestCommit() async {
    // lastPatcherCommit =
    //     await githubAPI.latestCommitTime("revanced", "revanced-patcher");
    // lastManagerCommit =
    //     await githubAPI.latestCommitTime("revanced", "revanced-manager");
  }

  @override
  void initState() {
    setState(() {
      latestCommit();
    });
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: const Color(0xff1B222B),
      ),
      padding: const EdgeInsets.symmetric(vertical: 18, horizontal: 20),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    "Patcher: ",
                    style: GoogleFonts.roboto(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    "$lastPatcherCommit ago",
                    style: robotoTextStyle,
                  )
                ],
              ),
              Row(
                children: [
                  Text(
                    "Manager: ",
                    style: GoogleFonts.roboto(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    "$lastManagerCommit ago",
                    style: robotoTextStyle,
                  )
                ],
              ),
            ],
          ),
          PatchTextButton(
            text: "Update Manager",
            onPressed: () {},
            backgroundColor: const Color(0xff7792BA),
          ),
        ],
      ),
    );
  }
}
