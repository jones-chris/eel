document.getElementById('fileUploadForm').addEventListener('submit', function(event) {
    console.log("Inside file upload submit event listener")
    event.preventDefault(); // Prevent the default form submission

    const formData = new FormData(this); // Create a FormData object from the form

    fetch('<presigned URL goes here>', { // Replace '/upload' with the S3 presigned URL.
        method: 'PUT',
        headers: {
            'Content-Type': 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        },
        body: formData
    })
    .then(response => {
        if (response.status !== 200) {
            throw Error(`Received status of ${response.status}`)
        }
    })
    .then(data => {
        alert('Workbook successfully uploaded.  Inspecting workbook.')
        console.log('Success:', data);
        // Handle success (e.g., show a message to the user)
    })
    .catch((error) => {
        console.error('Error:', error);
        // Handle error (e.g., show an error message)
    });
});
