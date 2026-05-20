import { apiBaseUrl } from './constants.js';

let flowId = null;
let flowVersion = null;


async function getPresignedUrl() {
    const extractionType = 'artifact';  // todo:  only allow artifact extraction types for now.

    let response = await fetch(`${apiBaseUrl}/flow/transformationLandingUrl?id=${flowId}&type=${extractionType}`, {
         method: 'GET'
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

async function buildArtifact() {
    let response = await fetch(`${apiBaseUrl}/manifest?uuid=${flowId}`, {
         method: 'PUT',
    });

    if (response.status === 404) {
        return null;
    } else if (response.status === 200) {
        let data = await response.json();
        console.log(`artifact data is ${JSON.stringify(data)}`);

        return data;
    } else {
        const errorMessage = `Received status of ${response.status} when building the artifact`;

        console.error(errorMessage);
        throw Error(errorMessage);
    }
}

// Get new flow's ID.
window.onload = function() {
    fetch(`${apiBaseUrl}/flow/new`, {
        method: 'POST',
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

    const fileInput = document.getElementById('fileInput');
    if (fileInput.files.length !== 1) {
        alert('Please select one file to upload.');
        toggleLoading();
        return;
    }
    const file = fileInput.files[0];

    let presignedUrl = await getPresignedUrl(flowId);

    let response = await fetch(presignedUrl, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        },
        body: file
    });
    if (response.status !== 200) {
        console.error('Error:', error);
        alert('There was an error uploading your file.  Please contact your administrator.');

        throw Error(`Received status of ${response.status}`)
    }

    // If the upload was successful, but the "Build Artifact Only" checkbox is checked, then we attempt to download the artifact and skip rendering the manifest.
    if (document.getElementById('uploadCheckbox').checked) {
        await sleep(30);
        alert("Building Artifact.  This may take a couple minutes. Please be patient and stay on this page.")

        try {
            // Attempts to build the artifact with backoff.
            let buildData = null;
            const maxAttempts = 5;
            let attemptNumber = 0;
            let sleepInSeconds = 10;
            do {
                buildData = await buildArtifact();
                if (buildData === null) {
                    attemptNumber++;
                    sleepInSeconds = sleepInSeconds * attemptNumber;
                } else {
                    break;
                }

                if (attemptNumber >= maxAttempts) {
                    alert('There was an error building the artifact.  Please contact your administrator');
                    break;
                }

                console.log(`Sleeping for ${sleepInSeconds} seconds`)
                await sleep(sleepInSeconds)
            } while (attemptNumber < maxAttempts)

            if (buildData !== null) {
                let presignedUrl = buildData.url;

                // Download the artifact
                let downloadLink = document.createElement('a');
                downloadLink.href = presignedUrl;
                downloadLink.download = 'artifact.jar';
                document.body.appendChild(downloadLink);
                downloadLink.click();
                document.body.removeChild(downloadLink);

                alert('Artifact built and download initiated.');
            }
        } catch (error) {
            console.error('Error building or downloading artifact:', error);
            alert('There was an error building the artifact. Please contact your administrator.');
        }

        toggleLoading();
        return; // Skip manifest rendering
    }
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
        "xlsxFile": "xlsxTransformationUpload"
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
