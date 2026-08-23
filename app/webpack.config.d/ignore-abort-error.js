if (config.devServer) {
    config.devServer.client = config.devServer.client || {};
    config.devServer.client.overlay = {
        runtimeErrors: (error) => {
            if (error && error.message && error.message.includes("AbortError")) {
                return false;
            }
            return true;
        }
    };
}
