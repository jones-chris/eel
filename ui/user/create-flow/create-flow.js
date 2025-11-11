let flowId = null;
let flowVersion = null;
let apiDomain = '';
let manifestApiDomain = '';
let inputSources = [];

async function getPresignedUrl() {
    let response = await fetch(`${apiDomain}/flow/transformationLandingUrl?id=${flowId}`, {
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
    let response = await fetch(`${manifestApiDomain}/manifest?uuid=${flowId}&version=${flowVersion}`, {
         method: 'GET'
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
    fetch(`${apiDomain}/flow/new`, {
        method: 'POST'
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

    const data = response.json();
    console.log(`Success: ${JSON.stringify(data)}`);

    // Attempts to get the manifest with backoff.
    let manifest = null;
    const maxAttempts = 5;
    let attemptNumber = 0;
    let sleepInSeconds = 4;
    do {
        manifest = await getManifest();
        if (manifest === null) {
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

    if (manifest !== null) {
        console.log('Visualizing manifest...');
    }

    toggleLoading();
});

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

fetch('./manifest-test.json')
    .then(response => response.json())
    .then(json => {
        let inputSourceRootElement = document.getElementById('inputSources');
        for (let inputSheetId in Object.keys(json['inputSheetsMetadata'])) {
            let inputSheetMetadata = json['inputSheetsMetadata'][inputSheetId];

            let inputSource = new InputSource(inputSheetMetadata);
            inputSources.push(inputSource);

            inputSourceRootElement.appendChild(inputSource);
        }
    });
