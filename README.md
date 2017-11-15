# Themis Plugin for Jenkins

A Jenkins plugin to automatically refresh a Themis project in a post-build step.

## Usage

### Create a Themis instance

In global configuration (“Manage Jenkins” ⇒ “Configure System”), add a Themis instance in the Themis configuration 
section. You need to fill the following fields:

- **instance name**: the name of the instance, used to identify it if there are several instances.
- **URL**: the base url of your Themis instance (e.g., “https://themis.example.com”).
- **API key**: a Themis API key, generated from the administration page of your Themis instance. Used for 
authentication.

### Refreshing Themis

In the configuration of your job, add the post-build action “Refresh Themis Project”. Select the Themis instance 
where the project to refresh is located, then the project key, which is available in the project administration page 
of Themis.

The post-build action will send a request for refreshing the project, but will not wait for its completion. You 
should thus check your Themis instance to see the refresh status. By default errors will not mark the build as 
failed, but you can change this behavior by checking the “Error fails build” checkbox.
