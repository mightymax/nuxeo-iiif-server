<!DOCTYPE html>
<html><head>
    <meta charset="utf-8">

    <!-- By default uses Roboto font. Be sure to load this or change the font -->
    <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:300,400,500">
    <link rel="shortcut icon" type="image/png" href="/images/logo16.png">
    <title>Nuxeo IIIF Manifest viewer</title>
    <script src="https://unpkg.com/mirador@latest/dist/mirador.min.js"></script>
    <style>
        html, body {
            width: 100%;
            height: 100%;
        }
        body, #viewer {
            margin: 0;
            padding: 0;
            height: 100%;
        }
    </style>
</head>

<body>
<div id="viewer"></div>
<script type="text/javascript">
    var mirador = Mirador.viewer({
      "id": "viewer",
      "manifests": {
        "${manifest}": {
        "provider": "Memorix IIIF Demonstrator"
        }
    },
      "windows": [
        {
            "loadedManifest": "${manifest}",
            "canvasIndex": 0,
            "thumbnailNavigationPosition": "far-bottom"
        }
    ]
});
    </script>
</body>
</html>
