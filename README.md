# IIIF Manifest builder for Nuxeo

Serves as a API server to generate Manifest files build from Nuxeo folders. Also contains a reverse proxy to Cantaloupe, so this server can act as a IIIF server as well.
Multi functional APi server:
1. Acts as a IIIF manifest generator for Nuxeo folders
2. Is a reverse proxy for Cantaloupe making the App a full IIIF Image 3.0 server
3. Acts as a reverse proxy to access Nuxeo Blob's endpoint `nuxeo/nxfile/{uid}`

## Examples
- Reverse proxy for Nuxeo Blob:
  - HEAD: <br>`curl -I http://localhost:8180/blob/746da230-5acd-463b-b667-eb537398f163`
  - Download: <br>`curl http://localhost:8180/blob/746da230-5acd-463b-b667-eb537398f163 --outout picture.jp2`
- Get IIIF Manifest for Nuxeo folders:
  - by path: <br>`curl -H 'Content-Type: application/json' http://localhost:8180/manifest/from/folder/by/path/default/workspaces/memorix/beeldbank/DAM/01/ams/27/CD_010122_2256_TIF`
  - by UID: <br>`curl -H 'Content-Type: application/json' http://localhost:8180/manifest/from/folder/by/uid/e0edbc53-1336-4328-9a66-1b412539390f`
  - by foldername (watch out for non-unique foldernames, the first found is used): <br>`curl -H 'Content-Type: application/json' http://localhost:8180/manifest/from/folder/by/name/CD_010122_2256_TIF`
  
## Mirador viewer
This App contains a HTML endpoint with the Mirador viewer to demonstrate the use of manifests. 
To access this viewer goto: 
- http://localhost:8180/viewer/folder/by/uuid/e0edbc53-1336-4328-9a66-1b412539390f
- http://localhost:8180/viewer/folder/by/path/default/workspaces/memorix/beeldbank/DAM/01/ams/27/CD_010122_2256_TIF
- http://localhost:8180/viewer/folder/by/name/CD_010122_2256_TIF
  
## Cantaloupe
Tested with Cantaloupe 5.0.2. To use this app with Cantaloupe, make sure to use these settings (among others...):
```properties
source.static = HttpSource
HttpSource.allow_insecure = true
HttpSource.lookup_strategy = BasicLookupStrategy
# point this to the URL/Port of this App:
HttpSource.BasicLookupStrategy.url_prefix = http://localhost:8180/blob/
HttpSource.chunking.enabled = true
delegate_script.enabled = false
processor.selection_strategy = ManualSelectionStrategy
processor.ManualSelectionStrategy.jp2 = KakaduNativeProcessor
```