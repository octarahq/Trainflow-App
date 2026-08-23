if (config.devServer) {
    config.devServer.client = {
        webSocketURL: 'auto://0.0.0.0:0/ws',
    };
    config.devServer.allowedHosts = "all";
    config.devServer.historyApiFallback = true;
    config.devServer.port = 8081;
}
