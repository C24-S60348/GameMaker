var g_sandboxMode = true;

function getCurrentApp() {
    if (g_sandboxMode) {
        return Windows.ApplicationModel.Store.CurrentAppSimulator;
    }
    return Windows.ApplicationModel.Store.CurrentApp;
}

function activateStore(_onComplete, _onError) {

    if (g_sandboxMode) {

        // The user might've already set a license file themselves that doesn't have "IsTrial" set
        // in which case purchases will already work locally
        var currentApp = Windows.ApplicationModel.Store.CurrentAppSimulator;
        var licenseInformation = currentApp.licenseInformation;
        if (licenseInformation.isTrial) {
            loadInAppPurchaseProxyFile(_onComplete, _onError);
        }
    }
    else {
        _onComplete();
    }
}

function loadInAppPurchaseProxyFile(_onComplete, _onError) {

    Windows.ApplicationModel.Package.current.installedLocation.getFolderAsync("Windows8\\data").done(
        function (folder) {
            folder.getFileAsync("PurchaseLicenceProxy.xml").done(
                function (file) {

                    var currentApp = Windows.ApplicationModel.Store.CurrentAppSimulator;
                    currentApp.reloadSimulatorAsync(file).done(function () {
                        var licenseInformation = currentApp.licenseInformation;
                        if (!licenseInformation.isActive || licenseInformation.IsTrial) {
                            _onError();
                        }
                        else {
                            _onComplete();
                        }
                    });
                });
        });
}

function purchaseContent(_id, _complete, _error) {

    var currentApp = getCurrentApp();
    try {
        currentApp.requestProductPurchaseAsync(_id, false).then(_complete, _error);
    }
    catch (e) {
        alert(e);
    }
}

function provideContent(_contentURL, _localPath, _complete, _error, _progress) {

    // Convert forward slashes to back slashes on the local path
    var localPath = _localPath.replace(/\//g, '\\');

    // Asynchronously create the file in the local folder.
    var applicationData = Windows.Storage.ApplicationData.current;
    var localFolder = applicationData.localFolder;
    
    var targetZip = _contentURL.split(/(\\|\/)/g).pop();
    localFolder.createFileAsync(targetZip, Windows.Storage.CreationCollisionOption.generateUniqueName).done(

        function (newFile) {
            var uri = Windows.Foundation.Uri(_contentURL);
            var downloader = new Windows.Networking.BackgroundTransfer.BackgroundDownloader();

            // Create a new download operation.
            download = downloader.createDownload(uri, newFile);
            download.startAsync().then(
                function () {
                    unzipContent(newFile, download, localPath, _complete, _error);
                },
                _error, _progress);
        },
        function (err) {
            _error(err);
        });
}

function unzipContent(_file, _download, _localPath, _complete, _error) {

    var contentType = _download.getResponseInformation().headers.lookup("Content-Type");
    if (contentType.indexOf("application/zip") === 0) {

        // Open the resultant downloaded file
        _download.resultFile.openAsync(Windows.Storage.FileAccessMode.read).done(function (stream) {
            
            var blob = MSApp.createBlobFromRandomAccessStream(_download.resultFile.contentType, stream);
            extractFilesFromZip(blob, _localPath, _complete, _error);
        });
    }
}

function extractFilesFromZip(_blob, _localPath, _complete, _error) {
    
    zip.createReader(new zip.BlobReader(_blob), 
        function (zipReader) {
            zipReader.getEntries(
                function (entries) {
                    unzipEntries(entries, _localPath, _complete, _error);
                },
                function () {
                    _error();
                });
        });
}

function unzipEntries(entries, _localPath, _complete, _error) {

    var fileList = [];
    var errored = false;
    var completionCount = 0;
    for (var n = 0; n < entries.length; n++) {

        var entry = entries[n];

        // write out the entry's contents to a local file accordingly
        var applicationData = Windows.Storage.ApplicationData.current;
        var localFolder = applicationData.localFolder;
        localFolder.createFileAsync(_localPath + '\\' + entry.filename, Windows.Storage.CreationCollisionOption.replaceExisting).done(
            function (file) {
                var writer = new zip.BlobWriter();
                entry.getData(writer,
                    function (blob) {                        
                        fileList[fileList.length] = file.name;
                        saveBlobToFile(file, blob,
                            function () {
                                completionCount = checkUnzipComplete(completionCount, entries.length, errored, fileList, _complete, _error);
                            },
                            function () {
                                errored = true;
                                completionCount = checkUnzipComplete(completionCount, entries.length, errored, fileList, _complete, _error);
                            });
                    },
                    function () { // getData progress
                    });
            },
            function (err) {
                errored = true;
                completionCount = checkUnzipComplete(completionCount, entries.length, errored, fileList, _complete, _error);
            });
    }
}

function saveBlobToFile(_file, _blob, _oncomplete, _onerror) {

    try {
        _file.openAsync(Windows.Storage.FileAccessMode.readWrite).then(
            function (output) {
                // Get the IInputStream stream from the blob object
                var input = _blob.msDetachStream();
                // Copy the stream from the blob to the File stream                
                Windows.Storage.Streams.RandomAccessStream.copyAsync(input, output).then(function () {
                    output.flushAsync().done(
                        function () {
                            input.close();
                            output.close();
                            _oncomplete();
                        },
                        function (err) {
                            _onerror();
                        });
                },
                function (err) {
                    _onerror();
                });
            }
        );
    }
    catch (e) {
        _onerror();
    }
}

function checkUnzipComplete(_count, _targetCount, _errored, _fileEntries, _oncomplete, _onerror) {

    _count++;
    if (_count == _targetCount) {
        if (_errored) {
            _onerror();
        }
        else {
            _oncomplete(_fileEntries);
        }
    }
    return _count;
}

function checkLicense(_id) {

    var currentApp = getCurrentApp();
    var licenseInformation = currentApp.licenseInformation;
    var license = licenseInformation.productLicenses.lookup(_id);

    return license.isActive;
}

function setLocalSetting(_key, _flag) {

    var applicationData = Windows.Storage.ApplicationData;
    var localSettings = applicationData.current.localSettings;
    localSettings.values[_key] = _flag;
}

function getLocalSetting(_key) {

    var applicationData = Windows.Storage.ApplicationData;
    var localSettings = applicationData.current.localSettings;
    if (localSettings.values[_key] != undefined) {
        return localSettings.values[_key];
    }
    return null;
}