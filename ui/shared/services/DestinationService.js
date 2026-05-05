export class DestinationService {

    constructor(apiDomain) {
        this.apiDomain = apiDomain;
    }

    async getDataSources() {
        let response = await fetch('./destinationDataSourceNames.json');
        
        if (response.status === 200) {
            let destinations = await response.json();
            return destinations.dataSources;
        }

        const message = `getDataSources response status code is ${response.status};`
        console.error(message);
        throw new Error(message);
    }

    async getDataSourceByName(name) {
        let response = await fetch('./destinationDataSourceNames.json');
        
        if (response.status === 200) {
            let destinations = await response.json();
            return destinations.dataSources[name];
        }

        const message = `getDataSourceByName response status code is ${response.status};`
        console.error(message);
        throw new Error(message);
    }

}