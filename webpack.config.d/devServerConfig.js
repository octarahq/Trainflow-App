if (config.devServer) {
    config.devServer.client = config.devServer.client || {};
    config.devServer.client.overlay = {
        errors: true,
        warnings: false,
        runtimeErrors: false
    };
}
