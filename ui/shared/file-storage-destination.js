class FileStorageDestination extends HTMLElement {

    #name;

    #parameters = {
        dataSource: null,
        fileName: null,
        storeEntireWorkbook: false
    }

    html() {
        return `
            <div>
               <div class="input-group mb-3">
                   <div class="input-group-prepend">
                       <label class="input-group-text">Data Sources</label>
                   </div>
                   <select id="dataSource_${this.name}" class="form-select">
                       <option value="">Choose...</option>
                       <option value="1">One</option>
                       <option value="2">Two</option>
                       <option value="3">Three</option>
                   </select>
               </div>

               <label for="fileName_${this.name}">File Name</label>
               <div>
                   <input id="fileName_${this.name}"></input>
               </div>

               <label for="storeEntireWorkbook_${this.name}">Store Entire Workbook</label>
               <div>
                   <input
                        id="storeEntireWorkbook_${this.name}"
                        type="checkbox"
                   >
                   </input>
               </div>
           <div>
        `;
    }

    constructor(outputDestination) {
        super();

        this.#name = `fileStorage_${outputDestination?.name}`;
    }

    registerHandlers(shadowRoot) {
        shadowRoot.getElementById(`dataSource_${this.name}`)
            .addEventListener('change', this.handleDataSourceChange.bind(this));

        shadowRoot.getElementById(`fileName_${this.name}`)
            .addEventListener('input', this.handleFileNameChange.bind(this));

        shadowRoot.getElementById(`storeEntireWorkbook_${this.name}`)
            .addEventListener('click', this.handleStoreEntireWorkbookChange.bind(this));
    }

    getParameters() {
        return this.#parameters;
    }

    handleDataSourceChange(event) {
        const newDataSource = (event.target.value === '') ? null : event.target.value;
        this.#parameters.dataSource = newDataSource;
    }

    handleFileNameChange(event) {
        const newFileName = event.target.value;
        this.#parameters.fileName = newFileName;
    }

    handleStoreEntireWorkbookChange(event) {
        const storeEntireWorkbook = event.target.checked;
        this.#parameters.storeEntireWorkbook = storeEntireWorkbook;
    }

}

customElements.define('file-storage-destination', FileStorageDestination);