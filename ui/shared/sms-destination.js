class SmsDestination extends HTMLElement {

    #name;

    #parameters = {
        phoneNumber: null,
        message: null
    }

    html() {
        return `
            <div>
               <label for="phoneNumber_${this.name}">Phone Number</label>
               <div>
                   <input id="phoneNumber_${this.name}"></input>
               </div>

               <label for="message_${this.name}">Message</label>
               <div>
                   <textarea id="message_${this.name}"></textarea>
               </div>
           <div>
        `
    }

    constructor(outputDestination) {
        super();

        this.#name = `sms_${outputDestination?.name}`;
    }

    registerHandlers(shadowRoot) {
        shadowRoot.getElementById(`phoneNumber_${this.name}`)
            .addEventListener('input', this.handlePhoneNumberChange.bind(this));

        shadowRoot.getElementById(`message_${this.name}`)
            .addEventListener('input', this.handleMessageChange.bind(this));
    }

    getParameters() {
        return this.#parameters;
    }

    handlePhoneNumberChange(event) {
        const newPhoneNumber = event.target.value;
        this.#parameters.phoneNumber = newPhoneNumber;
    }

    handleMessageChange(event) {
        const newMessage = event.target.value;
        this.#parameters.message = newMessage;
    }

}

customElements.define('sms-destination', SmsDestination);