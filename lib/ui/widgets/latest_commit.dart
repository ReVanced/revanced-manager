import 'package:flutter/material.dart';

class LatestCommitWidget extends StatelessWidget {
  const LatestCommitWidget({Key? key}) : super(key: key);

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
            children: const [
              Text("Patcher: 20 hours ago"),
              Text("Manager: 3 days ago"),
            ],
          ),
          TextButton(
            onPressed: () {},
            style: TextButton.styleFrom(
              primary: Colors.white,
              backgroundColor: const Color(0xff7792BA),
              padding: const EdgeInsets.symmetric(
                vertical: 10,
                horizontal: 24,
              ),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(24),
              ),
            ),
            child: const Text("Update Manager"),
          )
        ],
      ),
    );
  }
}
