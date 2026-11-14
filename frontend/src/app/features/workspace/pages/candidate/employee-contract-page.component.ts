import { Component, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { CandidateApplicationService } from '../../../public/services/candidate-application.service';

@Component({
  selector: 'app-employee-contract-page',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  templateUrl: './employee-contract-page.component.html',
  styleUrl: './employee-contract-page.component.scss'
})
export class EmployeeContractPageComponent implements OnInit {
  private readonly applications = inject(CandidateApplicationService);
  contract: Record<string, any> | null = null;
  loading = true;

  ngOnInit(): void {
    this.applications.employeeContract().subscribe({
      next: (contract) => { this.contract = contract; this.loading = false; },
      error: () => { this.contract = null; this.loading = false; }
    });
  }

}
