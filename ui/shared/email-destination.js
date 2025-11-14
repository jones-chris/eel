class EmailDestination extends HTMLElement {
 
    #name;

    #parameters = {
        subject: null,
        recipients: [],
        body: null,
    };

    html() {
        return `
           <div>
               <label for="subject_${this.name}">Subject</label>
               <div>
                   <input id="subject_${this.name}"></input>
               </div>

               <label for="recipients_${this.name}">Recipients</label>
               <div>
                   <input id="recipients_${this.name}" placeholder="finance@mycompany.com; hr@mycompany.com"></input>
               </div>

               <label for="body_${this.name}">Body</label>
               <div>
                   <textarea id="body_${this.name}"></textarea>
               </div>
           <div>
       `;
    }

    constructor(outputDestination) {
        super();

        this.#name = `email_${outputDestination?.name}`;
    }

    registerHandlers(shadowRoot) {
        const subjectElement = shadowRoot.getElementById(`subject_${this.name}`);
        subjectElement.addEventListener('input', this.handleSubjectChange.bind(this));

        const recipientsElement = shadowRoot.getElementById(`recipients_${this.name}`);
        recipientsElement.addEventListener('input', this.handleRecipientsChange.bind(this));

        const bodyElement = shadowRoot.getElementById(`body_${this.name}`);
        bodyElement.addEventListener('input', this.handleBodyChange.bind(this));
    }

    getParameters() {
        return this.#parameters;
    }

    handleSubjectChange(event) {
        const newSubject = event.target.value;
        this.#parameters.subject = newSubject;
    }

    handleRecipientsChange(event) {
        const newRecipients = event.target.value;
        this.#parameters.recipients = newRecipients;
    }

    handleBodyChange(event) {
        const newBody = event.target.value;
        this.#parameters.body = newBody;
    }

}


customElements.define('email-destination', EmailDestination);