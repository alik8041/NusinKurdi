import 'package:flutter/material.dart';
import 'package:webview_flutter/webview_flutter.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      title: 'نووسین بە کوردی',
      home: const WebAppHome(),
    );
  }
}

class WebAppHome extends StatefulWidget {
  const WebAppHome({super.key});

  @override
  State<WebAppHome> createState() => _WebAppHomeState();
}

class _WebAppHomeState extends State<WebAppHome> {
  late final WebViewController _controller;

  @override
  void initState() {
    super.initState();
    _controller = WebViewController()
      ..loadRequest(Uri.parse('https://nusinkurdi.com/app1/'))
      ..setJavaScriptMode(JavaScriptMode.unrestricted);
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Scaffold(
        body: WebViewWidget(controller: _controller),
      ),
    );
  }
}