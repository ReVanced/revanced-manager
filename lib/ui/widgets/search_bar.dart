import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

class SearchBar extends StatefulWidget {
  final String? hintText;
  final Color? backgroundColor;
  final Color? fillColor;
  final Color? hintTextColor;

  const SearchBar({
    required this.hintText,
    this.backgroundColor = const Color(0xff1B222B),
    this.hintTextColor = Colors.white,
    required this.fillColor,
    Key? key,
    required this.onQueryChanged,
  }) : super(key: key);

  final Function(String) onQueryChanged;

  @override
  State<SearchBar> createState() => _SearchBarState();
}

class _SearchBarState extends State<SearchBar> {
  final TextEditingController _textController = TextEditingController();
  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(12),
        color: widget.backgroundColor,
        border: Border.all(
          color: widget.backgroundColor != null
              ? widget.backgroundColor!
              : Colors.white,
          width: 1,
        ),
      ),
      child: Row(
        children: [
          Expanded(
            child: TextField(
              onChanged: widget.onQueryChanged,
              controller: _textController,
              decoration: InputDecoration(
                fillColor: widget.fillColor,
                filled: true,
                contentPadding: const EdgeInsets.all(12.0),
                hintText: widget.hintText,
                hintStyle: GoogleFonts.poppins(
                  color: widget.hintTextColor,
                  fontWeight: FontWeight.w400,
                ),
                prefixIcon: const Icon(
                  Icons.search,
                  size: 24.0,
                ),
                suffixIcon: _textController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        iconSize: 24.0,
                        onPressed: () {
                          _textController.clear();
                          widget.onQueryChanged('');
                        },
                      )
                    : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(10),
                  borderSide: BorderSide.none,
                ),
              ),
              style: GoogleFonts.poppins(
                color: Colors.white,
                fontWeight: FontWeight.w400,
                fontSize: 16,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
