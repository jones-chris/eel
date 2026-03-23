async function sleep(sleepInSeconds) {
    return new Promise(r => setTimeout(r, sleepInSeconds * 1000));
}

function removeAllChildNodes(target) {
    while (target.hasChildNodes()) {
        target.removeChild(target.firstChild);
    }
}

