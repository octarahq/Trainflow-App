const express = require('express');
const path = require('path');
const app = express();
require('dotenv').config({ path: path.join(__dirname, '.env') });

const wasmDistPath = path.join(__dirname, 'public');

app.use(express.static(wasmDistPath));

app.get('*', (req, res) => {
    res.sendFile(path.join(wasmDistPath, 'index.html'));
});

app.listen(process.env.SERVER_PORT, () => {
    console.log(`Trainflow PWA ${process.env.SERVER_PORT}`);
});
