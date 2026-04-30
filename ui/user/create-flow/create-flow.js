import { apiBaseUrl } from '../../constants/constants.js';
import { DestinationService } from '../../shared/services/DestinationService.js';
import { OutputDestination } from '../../shared/output-destination.js';
import { getAuthHeader, getUserName } from '../../utils/auth.js';

let flowId = null;
let flowVersion = null;
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
            Authorization: getAuthHeader()
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
            Authorization: getAuthHeader()
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
           Authorization: getAuthHeader()
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

// Add output destination.
document.getElementById('addOutputDestination').addEventListener('click', async function() {
    console.log('Inside add destination event listener');

    let outputDestination = new OutputDestination(destinationsService);
    state.outputDestinations.push(outputDestination);

    let outputDestinationsRootElement = document.getElementById('outputDestinations');
    outputDestinationsRootElement.appendChild(outputDestination);
});

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

    // If the upload was successful, but the "Build Artifact Only" checkbox is checked, then we attempt to download the artifact and skip rendering the manifest.
    if (document.getElementById('uploadCheckbox').checked) {
        try {
            // Call the /artifact/build endpoint
            let buildResponse = await fetch(`${apiBaseUrl}/artifact/build?uuid=${flowId}&version=${flowVersion}`, {
                method: 'POST',
                headers: {
                    Authorization: getAuthHeader()
                }
            });

            if (buildResponse.status !== 200) {
                throw new Error(`Failed to build artifact: ${buildResponse.status}`);
            }

            let buildData = await buildResponse.json();
            let presignedUrl = buildData.url;

            // Download the artifact
            let downloadLink = document.createElement('a');
            downloadLink.href = presignedUrl;
            downloadLink.download = 'artifact.jar'; // or whatever filename
            document.body.appendChild(downloadLink);
            downloadLink.click();
            document.body.removeChild(downloadLink);

            alert('Artifact built and download initiated.');
        } catch (error) {
            console.error('Error building or downloading artifact:', error);
            alert('There was an error building the artifact. Please contact your administrator.');
        }

        toggleLoading();
        return; // Skip manifest rendering
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
        renderManifest(state.manifest);
    }

    toggleLoading();
});

// Save Flow button listener.
document.getElementById('saveFlow').addEventListener('click', async function() {
    saveFlow(true);
});

// Deploy Flow button listener.
document.getElementById('deployFlow').addEventListener('click', async function() {
    saveFlow(false);

    // Send the data.
    const response = await fetch(`${apiBaseUrl}/flow/deploy?flowId={flowId}&version={version}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            Authorization: getAuthHeader()
        }
    })

    // Check if response is successful
    if (! response.ok) {
        alert('There was an error when deploying your flow.  Please try again later or contact your administrator.')
        throw new Error(`HTTP error! status: ${response.status}`);
    }

    // Parse and handle the response
    const result = await response.json();
    console.log('Success:', result);

    // Redirect to the flow details page
    window.location.href = `../flow/flow.html?id=${flowId}&version=${flowVersion}`;
});

// Add flow type event listener.
document.querySelectorAll('.dropdown-item').forEach(item => {
    item.addEventListener('click', function(e) {
        // Hide/unhide flow type input HTML elements.
        toggleFlowUploadElements(this.id)
    });
});

async function saveFlow(showAlert) {
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
        author: getUserName(),
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
            Authorization: getAuthHeader()
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

    // Because this function is called when saving a flow and when deploying a flow, we don't always want to display an alert.
    if (showAlert) {
        alert('Flow saved successfully!');
    }
}

// Hide/unhide flow type input HTML elements based on which flow type id is chosen.
function toggleFlowUploadElements(dropDownItemIdToShow) {
    const dropDownItemIdToFlowUploadElementIds = {
        "xlsxFile": "xlsxTransformationUpload",
        "sqlScript": "sqlScriptTransformationUpload",
        "pythonScript": "pythonScriptTransformationUpload",
        "pythonZipFile": "pythonZipFileTransformationUpload"
    };

    let flowUploadElementIdToShow = dropDownItemIdToFlowUploadElementIds[dropDownItemIdToShow];

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
        let inputSource = state.inputSources.find(inputSource => inputSource.name === inputSheetMetadata.name);
        if (inputSource) {
            newInputSources.push(inputSource);
        } else {
            inputSource = new InputSource(inputSheetMetadata);
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
