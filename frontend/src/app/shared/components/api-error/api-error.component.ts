import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ValidationError } from '../../../core/models/qrbill.model';

@Component({
  selector: 'app-api-error',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div *ngIf="errors && errors.length > 0" class="error-container">
      <div class="error-header">
        <span class="error-icon">⚠️</span>
        <strong>Validation / API Errors:</strong>
      </div>
      <ul class="error-list">
        <li *ngFor="let err of errors">
          <span class="field-name">{{ formatField(err.field) }}:</span> {{ err.message }}
        </li>
      </ul>
    </div>
  `,
  styles: [`
    .error-container {
      background-color: rgba(244, 63, 94, 0.1);
      border: 1px solid #f43f5e;
      border-radius: 8px;
      padding: 1rem;
      margin-bottom: 1.5rem;
      color: #f8fafc;
    }
    .error-header {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      margin-bottom: 0.5rem;
      color: #f43f5e;
    }
    .error-list {
      margin-left: 1.5rem;
      padding-left: 0;
    }
    .field-name {
      font-weight: 600;
      color: #94a3b8;
      text-transform: capitalize;
    }
  `]
})
export class ApiErrorComponent {
  @Input() errors: ValidationError[] = [];

  formatField(field: string): string {
    if (!field) return 'Error';
    return field
      .replace(/\./g, ' ')
      .replace(/([A-Z])/g, ' $1')
      .trim();
  }
}
