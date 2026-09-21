# Patent Management Platform - Design System Specification

## 1. Overview
This design system is based on the "Future Data" aesthetic, featuring a deep blue color palette, glowing effects, and high-contrast data visualization elements. It is built to be responsive, accessible (WCAG 2.1 AA compliant), and performant.

## 2. Color System

### Brand Colors (Dark Mode Default)
| Token | Value | Usage |
|-------|-------|-------|
| `--brand-deep-blue` | `#030829` | Global background, Header |
| `--brand-dark-blue` | `#081832` | Content surface |
| `--brand-teal-blue` | `#034c6a` | Panels, Cards, Borders |
| `--brand-bright-blue` | `#4b8df8` | Primary Actions, Focus, Highlights |
| `--brand-cyan` | `#25f3e6` | Data Vis, Secondary Accents |
| `--brand-yellow` | `#ffff43` | Warnings, Highlights |

### Semantic Mapping
The system uses semantic CSS variables to allow for theming (Light/Dark).

- **Backgrounds**:
  - `--bg-app`: App background (Deep Blue / Light Gray)
  - `--bg-surface`: Card/Panel background (Dark Blue / White)
  - `--bg-card-header`: Header background for cards

- **Text**:
  - `--text-primary`: Main content (White / Dark Gray)
  - `--text-secondary`: Subtitles (70% White / Gray)
  - `--text-muted`: Disabled/Hints (45% White / Light Gray)

## 3. Typography

**Font Stack**: `PingFang SC`, `Microsoft YaHei`, `Helvetica Neue`, sans-serif.

| Scale | Size | Variable | Usage |
|-------|------|----------|-------|
| Display | 36px | `--font-size-display` | Hero Headers |
| XXL | 28px | `--font-size-xxl` | Big Data Numbers |
| XL | 24px | `--font-size-xl` | Page Titles |
| LG | 18px | `--font-size-lg` | Card Titles |
| MD | 16px | `--font-size-md` | Body Text |
| SM | 14px | `--font-size-sm` | Secondary Text |
| XS | 12px | `--font-size-xs` | Labels, Metadata |

## 4. Layout & Spacing

### Grid System
- 12-column grid system (`.grid-container`).
- Responsive breakpoints:
  - Desktop: > 1024px (12 cols)
  - Tablet: 768px - 1024px (8 cols)
  - Mobile: < 768px (4 cols)

### Spacing Scale
Based on a 4px baseline.
- `--spacing-xs`: 4px
- `--spacing-sm`: 8px
- `--spacing-md`: 16px
- `--spacing-lg`: 24px
- `--spacing-xl`: 32px

## 5. Components & Effects

### The "Glowing Frame" Effect
Used for data panels to give a futuristic, inset-shadow look.
```css
.panel-glow {
  background: rgba(3, 76, 106, 0.2);
  border: 1px solid var(--brand-teal-blue);
  box-shadow: var(--shadow-glow-inset);
}
```

### Buttons
Primary buttons use a gradient background with a glow effect.
```css
.btn-primary {
  background: linear-gradient(135deg, var(--brand-bright-blue) 0%, var(--brand-teal-blue) 100%);
  border-radius: 9999px; /* Full Radius */
}
```

### Card Titles
Pill-shaped titles for data cards.
```css
.title-pill {
  background-color: var(--brand-teal-blue);
  border-radius: 18px;
  height: 35px;
  line-height: 35px;
}
```

## 6. Accessibility (WCAG 2.1 AA)
- **Contrast**: All text combinations meet the 4.5:1 ratio requirement.
- **Focus**: Interactive elements have a visible focus state (`--brand-bright-blue`).
- **Scaling**: Layouts use relative units (`rem`, `%`, `fr`) to support font scaling.
