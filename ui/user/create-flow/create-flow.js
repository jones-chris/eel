let flowId = null;
let flowVersion = null;
let apiBaseUrl = null;  // todo:  parameterize this.
const userName = null;  // todo:  parameterize this.
const password = null;  // todo:  paameterize this.
const state = {
    inputSources: [],
    outputDestinations: [],
    manifest: null
}
let flowType = null;
let destinationsService = new DestinationService(apiBaseUrl);


async function getPresignedUrl() {
    let response = await fetch(`${apiBaseUrl}/flow/transformationLandingUrl?id=${flowId}`, {
         method: 'GET',
         headers: {
            Authorization: "Basic " + btoa(userName + ":" + password)
         }
    });

    if (response.status !== 200) {
        const errorMessage = `Received status of ${response.status} when the presigned URL for the xlsx file`

        console.error(errorMessage);
        alert('There was an error generating the new flow.  Please contact your administrator');

        throw Error(errorMessage);
    }

    let data = await response.json();
    console.log(`data is ${JSON.stringify(data)}`);
    console.log(`flowId is now ${flowId}`);

    return data.url;
}

async function getManifest() {
    let response = await fetch(`${apiBaseUrl}/manifest?uuid=${flowId}&version=${flowVersion}`, {
         method: 'GET',
         headers: {
            Authorization: "Basic " + btoa(userName + ":" + password)
         }
    });

    if (response.status === 404) {
        return null;
    } else if (response.status === 200) {
        let data = await response.json();
        console.log(`manifest data is ${JSON.stringify(data)}`);

        return data;
    } else {
        const errorMessage = `Received status of ${response.status} when getting the manifest for this xlsx file`;

        console.error(errorMessage);
        alert('There was an error retrieving the xlsx manifest.  Please contact your administrator');

        throw Error(errorMessage);
    }
}

// Get new flow's ID.
window.onload = function() {
    fetch(`${apiBaseUrl}/flow/new`, {
        method: 'POST',
        headers: {
           Authorization: "Basic " + btoa(userName + ":" + password)
        }
    })
    .then(response => {
        if (response.status !== 201) {
            throw Error(`Received status of ${response.status} when getting the new flow ID`);
        }
        return response.json();
    })
    .then(data => {
        console.log(`data is ${JSON.stringify(data)}`);

        flowId = data.id;
        console.log(`flowId is now ${flowId}`);

        flowVersion = data.version;
        console.log(`flowVersion is now ${flowVersion}`);
    })
    .catch(error => {
        console.error(error);
        alert('There was an error generating the new flow.  Please contact your administrator');
    })
}

// Upload the transformation xlsx file.
document.getElementById('fileUploadForm').addEventListener('submit', async function(event) {
    console.log("Inside file upload submit event listener")

    toggleLoading();

    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form

    let presignedUrl = await getPresignedUrl(flowId);

    let response = await fetch(presignedUrl, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        },
        body: formData
    });
    if (response.status !== 200) {
        console.error('Error:', error);
        alert('There was an error uploading your file.  Please contact your administrator.');

        throw Error(`Received status of ${response.status}`)
    }

    alert('Workbook successfully uploaded.  Inspecting workbook.')

    // Attempts to get the manifest with backoff.
    let newManifest = null;
    const maxAttempts = 5;
    let attemptNumber = 0;
    let sleepInSeconds = 4;
    do {
        newManifest = await getManifest();
        if (newManifest === null) {
            attemptNumber++;
            sleepInSeconds = sleepInSeconds * attemptNumber;
        } else {
            break;
        }

        if (attemptNumber >= maxAttempts) {
            alert('There was an error retrieving the manifest.  Please contact your administrator');
            break;
        }

        console.log(`Sleeping for ${sleepInSeconds} seconds`)
        await sleep(sleepInSeconds)
    } while (attemptNumber < maxAttempts)

    if (newManifest !== null) {
        state.manifest = newManifest;

        console.log('Visualizing manifest...');
        renderManifest(manifest);
    }

    toggleLoading();
});

// Save Flow button listener.
document.getElementById('saveFlow').addEventListener('click', async function() {
    // Prepare the data.
    let sheetQueries = Object.fromEntries(
        state.inputSources.map(inputSource => [
            inputSource.name,
            {
                sql: inputSource.sql,
                dataSource: inputSource.dataSource
            }
        ])
    );

    const flow = {
        id: flowId,
        version: flowVersion,
        author: null, // todo:  fix this.
        inputType: 'SCHEDULED_BATCH',
        scheduledBatchConfiguration: {
            cronExpression: '',
            sheetQueries: sheetQueries
        }
    };

    // Send the data.
    const response = await fetch(`${apiBaseUrl}/flow/update`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
            Authorization: "Basic " + btoa(userName + ":" + password)
        },
        body: JSON.stringify(flow)
    })

    // Check if response is successful
    if (! response.ok) {
        alert('There was an error when saving your flow.  Please try again later or contact your administrator.')
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    // Parse and handle the response
    const result = await response.json();
    console.log('Success:', result);
    alert('Flow saved successfully!');
});


// Add flow type event listener.
document.querySelectorAll('.dropdown-item').forEach(item => {
    item.addEventListener('click', function(e) {
        // Hide/unhide flow type input HTML elements.
        toggleFlowUploadElements(this.id)
    });
});

// Hide/unhide flow type input HTML elements based on which flow type id is chosen.
function toggleFlowUploadElements(dropDownItemIdToShow) {
    const dropDownItemIdToFlowUploadElementIds = {
        "xlsxFile": "xlsxTransformationUpload",
        "sqlScript": "sqlScriptTransformationUpload",
        "pythonScript": "pythonScriptTransformationUpload",
        "pythonZipFile": "pythonZipFileTransformationUpload"
    };

    flowUploadElementIdToShow = dropDownItemIdToFlowUploadElementIds[dropDownItemIdToShow];

    Object.values(dropDownItemIdToFlowUploadElementIds).forEach(elementId => {
        const element = document.getElementById(elementId);
        if (elementId === flowUploadElementIdToShow) {
            element.removeAttribute('hidden');
        } else {
            element.setAttribute('hidden', '');
        }
    });
}

function toggleLoading() {
    const uploadSpinnerElement = document.getElementById('uploadSpinner');
    uploadSpinnerElement.hidden = ! uploadSpinnerElement.hidden;

    const uploadButtonElement = document.getElementById('uploadButtonText');
    if (uploadSpinnerElement.hidden) {
        uploadButtonElement.innerText = 'Upload';
    } else {
        uploadButtonElement.innerText = 'Inspecting...';
    }
}

function renderManifest(manifest) {
    // Render input sources.
    let newInputSources = [];
    let inputSourceRootElement = document.getElementById('inputSources');
    for (let inputSheetId in Object.keys(manifest['inputSheetsMetadata'])) {
        let inputSheetMetadata = manifest['inputSheetsMetadata'][inputSheetId];

        // Try to find the existing input source and keep it if it exists.
        let existingInputSource = state.inputSources.find(inputSource => inputSource.name === inputSheetMetadata.name);
        if (existingInputSource) {
            newInputSources.push(existingInputSource);
        } else {
            let inputSource = new InputSource(inputSheetMetadata);
            newInputSources.push(inputSource);
        }

        inputSourceRootElement.appendChild(inputSource);
    }
    state.inputSources = newInputSources;

    // Render output destinations.
//    let outputDestinationsRootElement = document.getElementById('outputDestinations');
//    for (let outputSheetId in Object.keys(manifest['outputSheetsMetadata'])) {
//        let outputSheetMetadata = manifest['outputSheetsMetadata'][outputSheetId];
//
//        let outputDestination = new OutputDestination(outputSheetMetadata, destinationsService);
//        state.outputDestinations.push(outputDestination);
//
//        outputDestinationsRootElement.appendChild(outputDestination);
//    }
}
