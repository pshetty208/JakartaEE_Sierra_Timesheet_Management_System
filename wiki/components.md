# Component Architecture

## Decisions

- Components should be `dumb` by default: accept attributes/facets and avoid owning page-specific content so that data can be served later by the backend and passed on to these dumb components.
- Examples live outside `resources/` so that they are accessible like JSF Pages. WEB-INF cannot be accessed by browser directly.

## Basic Component Structure

```text
web/
  resources/
    <component>/
      <component>.xhtml
      <component>.css
      <component>.js

  component-examples/
    index.xhtml
    <component>.xhtml

wiki/
  components.md
```

## Example

Declare the component library:

```xhtml
xmlns:sidebar="jakarta.faces.composite/sidebar"
```

Use attributes for configuration and facets for projected markup:

```xhtml
<sidebar:sidebar widgetVar="mainSidebar">
    <f:facet name="trigger">
        <button type="button" onclick="SierraComponents.sidebar.toggle('mainSidebar'); return false;">
            Open menu
        </button>
    </f:facet>

    <f:facet name="content">
        <p:menu style="width: 100%;">
            <p:menuitem value="Dashboard" outcome="/index" icon="pi pi-home"/>
        </p:menu>
    </f:facet>
</sidebar:sidebar>
```

## JavaScript

- Put shared interaction helpers in `<component>.js`.
- Expose one namespace, for example `SierraComponents.sidebar`.

### **\*\* Do not generate unique global functions per component instance** \*\*

 

## Important Endpoints

- App home: `Sierra_2026-war/`
- Main JSF page: `Sierra_2026-war/index.xhtml`
- Faces-mapped main page: `Sierra_2026-war/faces/index.xhtml`
- Component examples index: `Sierra_2026-war/component-examples/index.xhtml`

## Misc Architectural Knowledge

JSF pages must be rendered through the Faces Servlet. The app maps both `/faces/*` and `*.xhtml` to `jakarta.faces.webapp.FacesServlet` in `Sierra_2026-war/web/WEB-INF/web.xml` so component example pages work at both `/faces/component-examples/sidebar.xhtml` and `/component-examples/sidebar.xhtml`.

 

Do not remove the `*.xhtml` mapping. Without it, direct `.xhtml` URLs are served as raw static XHTML, so composite components, PrimeFaces widgets, and `<h:outputScript>` resources are not processed. A visible symptom is browser errors such as `SierraComponents is not defined` when a component button calls JavaScript from its resource file.