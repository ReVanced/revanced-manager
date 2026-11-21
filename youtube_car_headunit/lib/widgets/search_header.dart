import 'package:flutter/material.dart';
import 'package:lucide_icons/lucide_icons.dart';

import '../theme/car_theme.dart';

/// A category chip and the query it searches; a null query means trending.
typedef FeedCategory = ({String label, String? query});

/// Search field and category chips of the YouTube menu.
class SearchHeader extends StatefulWidget {
  const SearchHeader({
    super.key,
    required this.controller,
    required this.focusNode,
    required this.categories,
    required this.activeQuery,
    required this.onSearch,
    required this.onCategory,
    required this.onClear,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final List<FeedCategory> categories;

  /// Query the grid shows, to highlight the matching chip.
  final String activeQuery;
  final ValueChanged<String> onSearch;
  final ValueChanged<FeedCategory> onCategory;
  final VoidCallback onClear;

  @override
  State<SearchHeader> createState() => _SearchHeaderState();
}

class _SearchHeaderState extends State<SearchHeader> {
  bool _hasText = false;

  @override
  void initState() {
    super.initState();
    _hasText = widget.controller.text.isNotEmpty;
    widget.controller.addListener(_onTextChanged);
  }

  @override
  void didUpdateWidget(SearchHeader oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller != widget.controller) {
      oldWidget.controller.removeListener(_onTextChanged);
      widget.controller.addListener(_onTextChanged);
    }
  }

  void _onTextChanged() {
    final has = widget.controller.text.isNotEmpty;
    if (has != _hasText) setState(() => _hasText = has);
  }

  @override
  void dispose() {
    widget.controller.removeListener(_onTextChanged);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          height: 46,
          child: TextField(
            controller: widget.controller,
            focusNode: widget.focusNode,
            style: CarTheme.body,
            onSubmitted: widget.onSearch,
            textInputAction: TextInputAction.search,
            decoration: InputDecoration(
              hintText: 'Cari musik, lagu anak, kajian…',
              prefixIcon: const Icon(
                LucideIcons.search,
                size: 20,
                color: CarTheme.textMuted,
              ),
              suffixIcon: _hasText
                  ? IconButton(
                      icon: const Icon(LucideIcons.x, size: 18),
                      color: CarTheme.textMuted,
                      onPressed: widget.onClear,
                    )
                  : null,
            ),
          ),
        ),
        const SizedBox(height: 8),
        SizedBox(
          height: 38,
          child: ListView.separated(
            scrollDirection: Axis.horizontal,
            itemCount: widget.categories.length,
            separatorBuilder: (_, __) => const SizedBox(width: 8),
            itemBuilder: (context, index) {
              final category = widget.categories[index];
              final selected = category.query == widget.activeQuery ||
                  (category.query == null && widget.activeQuery.isEmpty);
              return _Chip(
                label: category.label,
                selected: selected,
                onTap: () => widget.onCategory(category),
              );
            },
          ),
        ),
      ],
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.label,
    required this.selected,
    required this.onTap,
  });

  final String label;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: selected ? CarTheme.text : CarTheme.surface,
      shape: StadiumBorder(
        side: BorderSide(color: selected ? CarTheme.text : CarTheme.border),
      ),
      child: InkWell(
        onTap: onTap,
        customBorder: const StadiumBorder(),
        child: Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Center(
            child: Text(
              label,
              style: CarTheme.body.copyWith(
                fontSize: 14,
                color: selected ? CarTheme.bg : CarTheme.text,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
