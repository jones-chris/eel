let flowId = null;
let apiDomain = '';

async function getPresignedUrl() {
    let response = await fetch(`${apiDomain}/flow/transformationLandingUrl?id=${flowId}`, {
         method: 'GET'
    });
    if (response.status !== 200) {
        throw Error(`Received status of ${response.status} when the presigned URL for the xlsx file`);

        console.error(error);
        alert('There was an error generating the new flow.  Please contact your administrator');
    }

    let data = await response.json();
    console.log(`data is ${data}`);
    console.log(`flowId is now ${flowId}`);

    return data.url;
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
        console.log(`data is ${data}`)
        flowId = data.id;
        console.log(`flowId is now ${flowId}`);
    })
    .catch(error => {
        console.error(error);
        alert('There was an error generating the new flow.  Please contact your administrator');
    })
}

// Upload the transformation xlsx file.
document.getElementById('fileUploadForm').addEventListener('submit', async function(event) {
    console.log("Inside file upload submit event listener")
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
    console.log('Success:', data);
});
