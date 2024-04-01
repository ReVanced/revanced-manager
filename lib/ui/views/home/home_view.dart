import 'dart:io';

import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:revanced_manager/app/app.locator.dart';
import 'package:revanced_manager/gen/strings.g.dart';
import 'package:revanced_manager/ui/views/home/home_viewmodel.dart';
import 'package:revanced_manager/ui/widgets/homeView/installed_apps_card.dart';
import 'package:revanced_manager/ui/widgets/homeView/latest_commit_card.dart';
import 'package:revanced_manager/ui/widgets/shared/custom_sliver_app_bar.dart';
import 'package:stacked/stacked.dart';

class HomeView extends StatelessWidget {
  const HomeView({super.key, this.adSize = AdSize.banner});

  final AdSize adSize;
  final String adUnitId = 'ca-app-pub-3940256099942544/6300978111';

  @override
  Widget build(BuildContext context) {
    return ViewModelBuilder<HomeViewModel>.reactive(
      disposeViewModel: false,
      fireOnViewModelReadyOnce: true,
      onViewModelReady: (model) => model.initialize(context),
      viewModelBuilder: () => locator<HomeViewModel>(),
      builder: (context, model, child) => Scaffold(
        body: RefreshIndicator(
          onRefresh: () => model.forceRefresh(context),
          child: CustomScrollView(
            slivers: <Widget>[
              CustomSliverAppBar(
                isMainView: true,
                title: Text(
                  t.homeView.widgetTitle,
                  style: GoogleFonts.inter(
                    color: Theme.of(context).textTheme.titleLarge!.color,
                  ),
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.all(20.0),
                sliver: SliverList(
                  delegate: SliverChildListDelegate.fixed(
                    <Widget>[
                      Text(
                        t.homeView.updatesSubtitle,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 10),
                      LatestCommitCard(model: model, parentContext: context),
                      const SizedBox(height: 23),
                      Text(
                        t.homeView.patchedSubtitle,
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      const SizedBox(height: 10),
                      InstalledAppsCard(),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class BannerAdWidget extends StatefulWidget {
  const BannerAdWidget({
    super.key,
    required this.adSize,
    required this.adUnitId,
  });
  
  final AdSize adSize;
  final String adUnitId;

  @override
  _BannerAdWidgetState createState() => _BannerAdWidgetState();
}

class _BannerAdWidgetState extends State<BannerAdWidget> {
  late BannerAd _bannerAd;

  @override
  void initState() {
    _loadAd();
    super.initState();
  }

  @override
  void dispose() {
    _bannerAd.dispose();
    super.dispose();
  }

  void _loadAd() {
    _bannerAd = BannerAd(
      size: widget.adSize,
      adUnitId: widget.adUnitId,
      request: const AdRequest(),
      listener: BannerAdListener(
        // Called when an ad is successfully received.
        onAdLoaded: (ad) {
          if (!mounted) {
            ad.dispose();
            return;
          }
          setState(() {
            _bannerAd = ad as BannerAd;
          });
        },
        // Called when an ad request failed.
        onAdFailedToLoad: (ad, error) {
          debugPrint('BannerAd failed to load: $error');
          ad.dispose();
        },
      ),
    );

    // Start loading.
    _bannerAd.load();
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: SizedBox(
        width: widget.adSize.width.toDouble(),
        height: widget.adSize.height.toDouble(),
        child: _bannerAd == null ? const SizedBox() : AdWidget(ad: _bannerAd),
      ),
    );
  }
}
