import 'package:flutter/material.dart';

class SearchBar extends StatefulWidget {
  final String? hintText;
  final bool showSelectIcon;
  final Function(bool)? onSelectAll;

  const SearchBar({
    Key? key,
    required this.hintText,
    this.showSelectIcon = false,
    this.onSelectAll,
    required this.onQueryChanged,
  }) : super(key: key);

  final Function(String) onQueryChanged;

  @override
  State<SearchBar> createState() => _SearchBarState();
}

class _SearchBarState extends State<SearchBar> {
  final TextEditingController _textController = TextEditingController();
  bool _toggleSelectAll = true;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(16),
        color: Theme.of(context).colorScheme.secondaryContainer,
      ),
      child: Row(
        children: <Widget>[
          Expanded(
            child: TextFormField(
              onChanged: widget.onQueryChanged,
              controller: _textController,
              style: TextStyle(
                color: Theme.of(context).colorScheme.secondary,
              ),
              decoration: InputDecoration(
                filled: true,
                fillColor: Theme.of(context).colorScheme.secondaryContainer,
                contentPadding: const EdgeInsets.all(12.0),
                hintText: widget.hintText,
                prefixIcon: Icon(
                  Icons.search,
                  color: Theme.of(context).colorScheme.secondary,
                ),
                suffixIcon: _textController.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _textController.clear();
                          widget.onQueryChanged('');
                        },
                      )
                    : widget.showSelectIcon
                        ? IconButton(
                            icon: _toggleSelectAll
                                ? const Icon(Icons.deselect)
                                : const Icon(Icons.select_all),
                            onPressed: widget.onSelectAll != null
                                ? () {
                                    setState(() {
                                      _toggleSelectAll = !_toggleSelectAll;
                                    });
                                    widget.onSelectAll!(_toggleSelectAll);
                                  }
                                : () => {},
                          )
                        : null,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(12),
                  borderSide: BorderSide.none,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
