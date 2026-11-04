# ADR-0005: Deploy to AWS with an IAM user's access keys instead of OIDC

## Context

The course's week 7 practice deploys a Lambda function from GitHub Actions. Its pipeline
signs in to AWS with OIDC: an IAM identity provider for `token.actions.githubusercontent.com`
and a role that GitHub assumes for temporary credentials, so no key is stored anywhere.

The team's AWS account was created with the new AWS experience: a project owned by an AWS
Builder ID, in `us-east-2`, with every member as an administrator. AWS manages the service
control policies of these accounts and they cannot be edited. The policy for the Free Tier
of projects, and the one for the Paid Plan, include a statement `DenyIAMRestrictedActions`
that denies `iam:*Provider*`, so the identity provider cannot be created:

```text
aws: [ERROR]: An error occurred (AccessDenied) when calling the CreateOpenIDConnectProvider operation: User: arn:aws:sts::<account-id>:assumed-role/AccountFullAccessRole/b4487428-1051-70b5-d15c-2ceff0da7c55 is not authorized to perform: iam:CreateOpenIDConnectProvider on resource: arn:aws:iam::<account-id>:oidc-provider/token.actions.githubusercontent.com with an explicit deny in a service control policy: arn:aws:organizations::<account-id>:policy/o-lxgjktlll2/service_control_policy/p-brm2as2d. Go to https://us-east-1.console.aws.amazon.com/iam/home?region=us-east-1#/authorization-details/5qj5p4rys5jebdjj4r0734r2w for complete details, or call the GetRequestAuthorizationDetails API with the following authorization id: 5qj5p4rys5jebdjj4r0734r2w
```

AWS publishes those policies in
[Managed policies for your organization](https://docs.aws.amazon.com/accounts/latest/reference/scps-and-rcps-for-projects.html).
Only activating advanced features removes this one, and AWS asks to upgrade to the paid plan
first; activation cannot be reversed and removes spend limits. The same policies do allow
IAM users for programmatic access (`iam:*LoginProfile*` is denied, so those users can never
sign in to the console).

## Decision

- **IAM user `gha-deploy-gestor-salon`**, programmatic access only, with one inline policy,
  `deploy-lambdas`. It has the permissions of the course's deploy role, limited to this
  project: get, update and invoke the functions `gestor-salon-*`, and push and read images
  in the ECR repositories `gestor-salon-*`, both in `us-east-2`, plus the ECR login token. It
  cannot create, delete or configure functions, change IAM or use any other service.
- **One access key**, created by the repository owner and pasted straight into GitHub. It is
  never sent through chat, saved to a file or shown in a screenshot.
- **GitHub environment `aws`** keeps the key as the secrets `AWS_ACCESS_KEY_ID` and
  `AWS_SECRET_ACCESS_KEY`. Its deployment branch rule only allows `main`, which does the job
  of the role's trust condition: a pull request or any other branch cannot read the key.
- The rest follows the course: tests inside the image build, image tagged with the short
  commit SHA, ECR keeps the last 3 images, smoke test after every deploy.

## Alternatives considered

| Option | Why not |
| :--- | :--- |
| OIDC, as in the course | The AWS-managed policy denies creating the identity provider |
| Activate advanced features | Needs the paid plan first, cannot be undone and removes spend limits |
| Another AWS account with the classic sign-up | The project, its budget and the team's access already live in this one |
| AWS CodeBuild connected to GitHub | One more service to set up; the course's pipeline is GitHub Actions |
| Deploy from a laptop | Not continuous delivery: the step the practice automates goes back to being manual |

## Trade-offs / consequences

- The key does not expire: if it leaks, it works until someone deletes it. It can only
  deploy this project's functions, only `main` can read it, GitHub masks it in the logs,
  CloudTrail records every call it makes and the 5 USD budget alerts the team.
- Rotation: create a second key, replace both secrets, run the workflow, delete the old key.
  At the end of the course, or right away if a leak is suspected.
- Every function of the project reuses the same user and environment, as long as its names
  start with `gestor-salon-`: the image function now, and later the week 8 monitor.
- If the account ever activates advanced features, the user is replaced with the OIDC role
  of the course and the key is deleted.

## Validation evidence

- The `AccessDenied` above, from AWS CloudShell.
- `aws iam simulate-principal-policy` for the user: `allowed` for `lambda:UpdateFunctionCode`
  and `lambda:InvokeFunction` on `gestor-salon-imagenes` and for `ecr:PutImage` on its
  repository; `implicitDeny` for `lambda:CreateFunction`, `lambda:DeleteFunction`,
  `lambda:UpdateFunctionConfiguration`, `ecr:DeleteRepository` and for updating any other
  function.
- First deploy with these credentials: linked in `docs/evidence/sprint-08.md`.