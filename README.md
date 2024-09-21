# Hello Cyoda Application

Welcome to the “Hello-Cyoda” application for Cyoda, a cloud-based platform designed to simplify building applications using entity-based workflows. This repository is a **Kotlin** example application to connect with a Cyoda environment and demonstrates basic functions through interactive Jupyter notebooks.

Other languages you can find on our [Cyoda-platform GitHub](https://github.com/Cyoda-platform).



## Overview

This project demonstrates how to connect to the Cyoda environment and execute basic operations such as managing models, entities, and workflows. The example shows how to authenticate, manage access tokens, and perform CRUD operations on entities.

## Prerequisites

1. A Cyoda Platform account with access to an environment (namespace).
1. Python 3.12.3 and Jupyter Notebook installed.
1. Kotlin installed (for the application backend). 
1. Access to your Cyoda environment’s API URL and credentials.

## Running in GitHub Codespaces

### Step 1: Clone the Repository

```bash
git clone https://github.com/Cyoda-platform/caas-hello-world.git
cd caas-hello-world
```

### Step 2: Set Up Environment Variables

In your **GitHub Codespaces**, set the `DEMO_USER_PASSWD` secret to your Cyoda password using the Codespace Secrets feature.

### Step 3: Run the Application in Codespaces

You can run the example Cyoda client directly from a Codespace terminal using Maven:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--cyoda.connection.client-id=demo.user --cyoda.connection.client-secret='${DEMO_USER_PASSWD}' --cyoda.connection.grpc-server=grpc-applifting.cyoda.net --cyoda.connection.api-url=https://applifting.cyoda.net/api"
```

### Step 4: Running the Jupyter Notebooks

#### 1. **Getting Started with Cyoda**

This notebook walks you through the basics, such as:
- Setting up API connections.
- Authenticating with the Cyoda environment using a refresh token.
- Managing models (checking if they exist, unlocking, locking, deleting).
- Creating and updating entities.

Start this notebook in your Codespace terminal with:

```bash
jupyter notebook getting-started-with-cyoda.ipynb
```

#### 2. **Bootstrapping Your Cyoda Environment**

This notebook demonstrates:
- Importing workflow configurations into your Cyoda environment.
- Registering entity models to save and retrieve entities.
- Running searches and processing results.

Start the notebook with:

```bash
jupyter notebook bootstrap_cyoda_env.ipynb
```

## Features

### Example API Interactions

- **Login and Token Management**: Authenticate with the Cyoda environment and manage access tokens.
- **Model Management**: Check if a model exists, unlock or lock it, and delete or reset models as needed.
- **Entity Management**: Create and update entities using entity workflows and search entities using Cyoda’s APIs.

### Run it in your favorite IDE

While the notebooks provide a good intro to Cyoda, we encourage you to explore the Kotlin application in your own development environment. Running it in your favorite IDE (such as IntelliJ IDEA or Eclipse) will give you a deeper understanding of how the integration works, and allow you to experiment with the core functions of Cyoda in a more flexible way.

We'll be covering soon:

- **Acting as a Predicate**: Your code can be invoked by Cyoda to make decisions on transitions, determining whether an entity can move from one state to another based on your business logic.
- **Transforming Data**: Implement logic to transform incoming data into your business entities, to simplify getting data in using **COBI** or your own approach.
- **Access Control**: Be called upon to make access decisions, ensuring proper permissioning for actions performed on entities.

These  scenarios are part of Cyoda’s flexible integration model, allowing you to hook into the platform’s workflow, data transformation, and security layers. More features like these will be demonstrated in upcoming examples and notebooks.

## Conclusion

This "Hello-Cyoda" application introduces the core functions of Cyoda, helping you manage workflows, entities, and data through both interactive notebooks and a Kotlin backend. It serves as a foundation to explore the powerful capabilities of Cyoda’s entity-based workflows for building cloud-native applications.

---

Enjoy building with Cyoda!

---